package com.github.drfiveminusmint.brickball.listener;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (playerMatch == null) return;
        if (player.getInventory().contains(Material.BRICK)) {
            playerMatch.checkScoring(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (playerMatch == null) return;
        //Override vanilla death functionality
        event.setCancelled(true);
        playerMatch.sendMessage(event.deathMessage());
        if ((event.getDamageSource().getCausingEntity() instanceof  Player && player.getInventory().contains(Material.BRICK))) {
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.setGlowing(false);
            player.getInventory().remove(Material.BRICK);
            ((Player) event.getDamageSource().getCausingEntity()).getInventory().addItem(new ItemStack(Material.BRICK, 1));
            event.getDamageSource().getCausingEntity().setGlowing(true);
            ((Player) event.getDamageSource().getCausingEntity()).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0));
            playerMatch.sendMessage(((Player) event.getDamageSource().getCausingEntity()).displayName().append(Component.text(" has the brick!",NamedTextColor.WHITE)));
        }
        if (player.getRespawnLocation() == null) Brickball.getInstance().getLogger().log(Level.SEVERE, "UH OH");
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(player.getRespawnLocation());
                player.heal(20);
                player.setFoodLevel(20);
            }
        }.runTask(Brickball.getInstance());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getItemDrop().getItemStack().getType().equals(Material.BRICK)) return;
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(event.getPlayer());
        if (playerMatch == null) return;
        // Don't allow players to drop the brick
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickup(PlayerAttemptPickupItemEvent event) {
        if (!event.getItem().getItemStack().getType().equals(Material.BRICK)) return;
        Player player = event.getPlayer();
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (playerMatch == null) return;
        playerMatch.sendMessage(player.displayName().append(Component.text(" has the brick!",NamedTextColor.WHITE)));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0));
        player.setGlowing(true);
    }

    @EventHandler
    public void onCrossbowFire(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (Brickball.getInstance().getMatchManager().getMatchByPlayer(player) == null) return;
        if (player.getInventory().contains(Material.BRICK)) {
            if (event.getBow().getItemMeta() instanceof CrossbowMeta crossbowMeta) {
                crossbowMeta.addChargedProjectile(new ItemStack(Material.ARROW));
                event.getBow().setItemMeta(crossbowMeta);
                player.playSound(Sound.sound(Key.key("item.crossbow.loading_end"), Sound.Source.BLOCK, 10f, 5f));
            }
            player.sendMessage("[Brickball] The brick prevents the use of crossbows!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    // Normalize Crossbow damage
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getDamageSource().getDamageType().equals(DamageType.ARROW)) return;
        if (Brickball.getInstance().getMatchManager().getMatchByPlayer(player) == null) return;
        event.setDamage(9.0);
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!(player.getInventory().getItem(event.getHand()).getType().equals(Material.BRICK) ||
                player.getInventory().getItem(event.getHand().getOppositeHand()).getType().equals(Material.BRICK))) return;
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (playerMatch == null) return;
        if (!(event.getRightClicked() instanceof Player otherPlayer)) return;
        if (playerMatch.getPlayerTeam(player) == playerMatch.getPlayerTeam(otherPlayer)) {
            player.getInventory().remove(Material.BRICK);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.setGlowing(false);
            otherPlayer.getInventory().addItem(new ItemStack(Material.BRICK, 1));
            otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0));
            otherPlayer.setGlowing(true);
            playerMatch.sendMessage(otherPlayer.displayName().append(Component.text(" has the brick!",NamedTextColor.WHITE)));
        }
    }
}

package com.github.drfiveminusmint.brickball.listener;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.brickball.match.MatchState;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
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
        playerMatch.checkDeathRegions(player);
        if (player.getInventory().contains(Material.BRICK) && player.getGameMode().equals(GameMode.ADVENTURE)) {
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
        playerMatch.reportDeath(player);
        if (((player.getKiller() != null && player.getKiller().getGameMode().equals(GameMode.ADVENTURE)) && (player.getInventory().contains(Material.BRICK) || player.getInventory().getItemInOffHand().getType().equals(Material.BRICK)))) {
            // Transfer the brick to the killer if there is one
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.setGlowing(false);
            player.getInventory().remove(Material.BRICK);
            if (player.getInventory().getItemInOffHand().getType().equals(Material.BRICK))
                player.getInventory().setItemInOffHand(null);
            player.getKiller().getInventory().addItem(new ItemStack(Material.BRICK, 1));
            player.getKiller().setGlowing(true);
            player.getKiller().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0));
            playerMatch.sendMessage(player.getKiller().displayName().append(Component.text(" has the BRICK!",NamedTextColor.WHITE)));
            // Start shot clock (if enabled)
            playerMatch.startShotClock();
        } else if ((playerMatch.getSettings().getBoolean(MatchSettings.Setting.BRICK_FUMBLING) || !playerMatch.getSettings().getBoolean(MatchSettings.Setting.RESPAWNING)) && (player.getInventory().contains(Material.BRICK) || player.getInventory().getItemInOffHand().getType().equals(Material.BRICK))) {
            // Reset brick if fumbling is enabled or respawning is disabled
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.setGlowing(false);
            player.getInventory().remove(Material.BRICK);
            if (player.getInventory().getItemInOffHand().getType().equals(Material.BRICK))
                player.getInventory().setItemInOffHand(null);
            playerMatch.spawnBrick();
            playerMatch.stopShotClock();
        } else if (playerMatch.getSettings().getBoolean(MatchSettings.Setting.DEATH_TURNOVERS) && player.getInventory().contains(Material.BRICK)) {
            // Start a turnover if death turnovers is enabled
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.setGlowing(false);
            player.getInventory().remove(Material.BRICK);
            if (player.getInventory().getItemInOffHand().getType().equals(Material.BRICK))
                player.getInventory().setItemInOffHand(null);
            // turn over the brick
            final Location cachedLoc = player.getRespawnLocation();
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.teleport(cachedLoc);
                }
            }.runTaskLater(Brickball.getInstance(), 1);
            playerMatch.turnover(player);
            return;
        }
        if (player.getRespawnLocation() == null) Brickball.getInstance().getLogger().log(Level.SEVERE, "UH OH");
        if (playerMatch.getSettings().getInt(MatchSettings.Setting.RESPAWN_DELAY) != 0)
            player.setGameMode(GameMode.SPECTATOR);
        if (playerMatch.getSettings().getBoolean(MatchSettings.Setting.RESPAWNING)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (playerMatch.getState().equals(MatchState.PAUSED)) return;
                    player.setGameMode(GameMode.ADVENTURE);
                    player.teleport(player.getRespawnLocation());
                    player.heal(20);
                    player.setFoodLevel(20);
                    player.setSaturation(playerMatch.getSettings().getInt(MatchSettings.Setting.SATURATION));
                    player.setFireTicks(0);
                }
            }.runTaskLater(Brickball.getInstance(), playerMatch.getSettings().getInt(MatchSettings.Setting.RESPAWN_DELAY));
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            // don't softlock if all players are dead
            boolean playerAlive = false;
            for (Audience audience : playerMatch.audiences())
                if (audience instanceof Player p && p.getGameMode().equals(GameMode.ADVENTURE)) playerAlive = true;
            if (!playerAlive) new BukkitRunnable() {
                public void run () {
                    playerMatch.startRound();
                }
            }.runTaskLater(Brickball.getInstance(), 20);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getItemDrop().getItemStack().getType().equals(Material.BRICK)) return;
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(event.getPlayer());
        if (playerMatch == null) return;
        // Don't allow players to drop the brick
        playerMatch.sendMessage(Component.text("[BRICK] You cannot be rid of me so easily..."));
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickup(PlayerAttemptPickupItemEvent event) {
        if (!event.getItem().getItemStack().getType().equals(Material.BRICK)) return;
        Player player = event.getPlayer();
        BrickballMatch playerMatch = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (playerMatch == null) return;
        playerMatch.sendMessage(player.displayName().append(Component.text(" has the BRICK!",NamedTextColor.WHITE)));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0));
        player.setGlowing(true);
        // Start shot clock if enabled.
        playerMatch.startShotClock();
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
            player.sendMessage("[Brickball] The BRICK prevents the use of crossbows!");
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
            if (player.getInventory().getItemInOffHand().getType().equals(Material.BRICK))
                player.getInventory().setItemInOffHand(null);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.setGlowing(false);
            otherPlayer.getInventory().addItem(new ItemStack(Material.BRICK, 1));
            otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION, 0));
            otherPlayer.setGlowing(true);
            playerMatch.sendMessage(otherPlayer.displayName().append(Component.text(" has the brick!",NamedTextColor.WHITE)));
        }
    }

    //Ensure players leave all matches before they disconnect.
    @EventHandler
    public void onPlayerLogout (PlayerQuitEvent event) {
        Brickball.getInstance().getMatchManager().leaveMatch(event.getPlayer());
    }
}

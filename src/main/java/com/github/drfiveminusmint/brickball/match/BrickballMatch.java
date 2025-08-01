package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.BrickballArena;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.sk89q.worldedit.world.World;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class BrickballMatch implements ForwardingAudience {
    private int matchID = 0;
    private Scoreboard scoreboard;
    private Objective objective;
    private Team[] teams = new Team[3];
    private BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE};
    private String[] teamNames = {"Team 1", "Team 2", "Spectators"};
    private HashSet<Player> players = new HashSet();
    private BrickballArena arena;

    private boolean running, paused;

    public BrickballMatch(ArenaTemplate template, World gameWorld) {
        arena = new BrickballArena(template, gameWorld);
    }

    public BrickballMatch(ArenaTemplate template, Location minPoint, int matchID) {
        this.matchID = matchID;
        arena = new BrickballArena(template, minPoint, matchID);
    }

    public void initialize() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("match score", Criteria.DUMMY, Component.text("Points"));
        for (int i = 0; i <= 1; i++) {
            teams[i] = scoreboard.registerNewTeam(teamNames[i]);
            teams[i].color(teamColors[i].textColor);
            teams[i].addEntry(teamNames[i]);
            objective.getScore(teamNames[i]).setScore(0);
        }
        teams[2] = scoreboard.registerNewTeam(teamNames[2]);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setAutoUpdateDisplay(true);
        arena.generateArena();
    }

    public boolean startMatch () {
        if (running) return false;
        //TODO more elegant handling
        for (int i = 0; i <= 1; i++)
            for (String string : teams[i].getEntries()) {
                Player player = Bukkit.getServer().getPlayer(string);
                if (player == null) continue;
                PlayerInventory inventory = player.getInventory();
                inventory.clear();
                inventory.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                inventory.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                inventory.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                inventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));
                LeatherArmorMeta meta = (LeatherArmorMeta) inventory.getBoots().getItemMeta();
                meta.setColor(teamColors[i].bukkitColor);
                meta.setUnbreakable(true);
                for (ItemStack armorPiece : inventory.getArmorContents()) {
                    armorPiece.setItemMeta(meta);
                }
                inventory.addItem(new ItemStack(Material.IRON_SWORD));
                inventory.addItem(new ItemStack(Material.CROSSBOW));
                inventory.addItem(new ItemStack(Material.COOKED_BEEF, 8));
                inventory.addItem(new ItemStack(Material.ARROW, 10));
                player.updateInventory();
            }
        running = true;
        startRound();
        return true;
    }

    public void startRound() {
        arena.closeDoors();
        for (int i = 0; i <= 1; i++)
            for (String string : teams[i].getEntries()) {
                Player player = Bukkit.getServer().getPlayer(string);
                if (player == null) continue;
                player.teleport(arena.getSpawnLocation(i));
                // Turn the player to face the brick before we set their respawn location
                player.lookAt(arena.getBrickSpawn(), LookAnchor.EYES);
                player.setGameMode(GameMode.ADVENTURE);
                player.heal(20);
                player.setFoodLevel(20);
                player.setRespawnLocation(player.getLocation(), true);
            }
        Location brickSpawn = arena.getBrickSpawn();
        Item brick = (Item) brickSpawn.getWorld().spawnEntity(brickSpawn, EntityType.ITEM);
        brick.setItemStack(new ItemStack(Material.BRICK, 1));
        // STAY THERE
        brick.setVelocity(new Vector(0, 0, 0));
        brick.setGlowing(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                arena.openDoors();
            }
        }.runTaskLater(Brickball.getInstance(), 200);
    }

    public void checkScoring(Player player) {
        for (int i = 0; i <= 1; i++)
            if (teams[i].hasPlayer(player))
                if (arena.checkLocationInSpawn(player.getLocation(), 1-i)) {
                    objective.getScore(teamNames[i]).setScore(objective.getScore(teamNames[i]).getScore() + 1);
                    player.getInventory().remove(Material.BRICK);
                    player.setGlowing(false);
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                    playSound(Sound.sound(Key.key("block.glass.break"), Sound.Source.BLOCK, 10f, 5f));
                    startRound();
                }
    }

    public boolean joinMatch(Player player) {
        teams[teams.length-1].addPlayer(player);
        players.add(player);
        player.setScoreboard(scoreboard);
        sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" joined the match.")));
        return true;
    }

    public boolean leaveMatch(Player player) {
        for (Team team : teams)
            team.removePlayer(player);
        // Don't take the brick with you when leaving
        if (player.getInventory().contains(Material.BRICK)) {
            Location brickSpawn = arena.getBrickSpawn();
            Item brick = (Item) brickSpawn.getWorld().spawnEntity(brickSpawn, EntityType.ITEM);
            brick.setItemStack(new ItemStack(Material.BRICK, 1));
            // STAY THERE
            brick.setVelocity(new Vector(0, 0, 0));
            brick.setGlowing(true);
        }
        player.setGlowing(false);
        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setRespawnLocation(player.getWorld().getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(player.getRespawnLocation());
        sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" left the match.")));
        return players.remove(player);
    }
    public boolean joinTeam(Player player, int teamID) {
        if (!players.contains(player))
            if (!joinMatch(player)) return false;
        if (teamID >= teams.length) return false;
        // Remove the player from any current teams
        for(Team team : teams)
            team.removePlayer(player);
        teams[teamID].addPlayer(player);
        if (teamID < teams.length-1)
            sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(String.format(" joined Team %d.", teamID+1))));
        else
            sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" is now spectating")));
        return true;
    }

    public boolean setTeamColor(BrickballColor color, int teamID) {
        if (teamID >= teamColors.length)
            return false;
        teams[teamID].color(color.textColor);
        teamColors[teamID] = color;
        arena.setTeamColor(color, teamID);
        return true;
    }

    public void shutdown () {
        for (Team team : teams) {
            Set<String> entries = team.getEntries();
            team.removeEntries(entries);
            team.unregister();
        }
        arena.cleanupArena();
        sendMessage(Component.text("[Debug] Shutdown Successful"));
        for (Player player : players) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            player.setGlowing(false);
            player.setGameMode(GameMode.SURVIVAL);
            player.setRespawnLocation(player.getWorld().getSpawnLocation());
            player.teleport(player.getRespawnLocation());
        }
        players.clear();
    }

    @Nullable
    public Team getPlayerTeam (Player player) {
        for(Team team : teams)
            if (team.hasPlayer(player)) return team;
        return null;
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return players;
    }

    public int getMatchID() {
        return matchID;
    }
}

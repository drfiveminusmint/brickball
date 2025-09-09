package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.BrickballArena;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class BrickballMatch implements ForwardingAudience {
    private final int matchID;
    private Scoreboard scoreboard;
    private Objective objective;
    private final Team[] teams = new Team[3];
    private final BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE};
    private final String[] teamNames = {"Team 1", "Team 2", "Spectators"};
    private final HashSet<Player> players = new HashSet<>();
    private final BrickballArena arena;
    private final MatchSettings settings;

    private static final Set<EntityType> CLEARING_ENTITIES = Set.of(EntityType.ITEM, EntityType.ARROW);

    private boolean running, paused;

    public BrickballMatch(ArenaTemplate template, Location minPoint, int matchID) {
        this.matchID = matchID;
        arena = new BrickballArena(template, minPoint, matchID);
        settings = new MatchSettings();
    }

    public void initialize() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("match score", Criteria.DUMMY, Component.text("Points"));
        for (int i = 0; i <= 1; i++) {
            teams[i] = scoreboard.registerNewTeam(teamNames[i]);
            teams[i].color(teamColors[i].textColor);
            teams[i].addEntry(teamNames[i]);
            teams[i].setAllowFriendlyFire(false);
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
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                ItemMeta swordMeta = sword.getItemMeta();
                swordMeta.setUnbreakable(true);
                sword.setItemMeta(swordMeta);
                inventory.addItem(sword);
                inventory.addItem(new ItemStack(Material.CROSSBOW));
                player.updateInventory();
            }
        //Setup spectators
        for (String string : teams[teams.length-1].getEntries()) {
            Player player = Bukkit.getServer().getPlayer(string);
            if (player == null) continue;
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(arena.getBrickSpawn());
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
                if (settings.getBoolean(MatchSettings.Setting.NATURAL_REGENERATION)) {
                    player.setUnsaturatedRegenRate(80);
                    player.setSaturatedRegenRate(10);
                } else {
                    player.setUnsaturatedRegenRate(Integer.MAX_VALUE);
                    player.setSaturatedRegenRate(Integer.MAX_VALUE);
                }
                player.heal(20);
                player.setFoodLevel(20);
                player.setSaturation(settings.getInt(MatchSettings.Setting.SATURATION));
                player.setFireTicks(0);
                if (settings.getInt(MatchSettings.Setting.STEAKS) > 0) {
                    player.getInventory().remove(Material.COOKED_BEEF);
                    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, settings.getInt(MatchSettings.Setting.STEAKS)));
                }
                if (settings.getInt(MatchSettings.Setting.ARROWS) > 0) {
                    player.getInventory().remove(Material.ARROW);
                    player.getInventory().addItem(new ItemStack(Material.ARROW, settings.getInt(MatchSettings.Setting.ARROWS)));
                }
                for (ItemStack itemStack : player.getInventory()) {
                    if (itemStack == null) continue;
                    if (itemStack.getItemMeta() instanceof CrossbowMeta meta) {
                        meta.setUnbreakable(true);
                        meta.setChargedProjectiles(null);
                        itemStack.setItemMeta(meta);
                    }
                }
                //prevent item smuggling
                ItemStack offhandItem = player.getInventory().getItemInOffHand();
                if (offhandItem != null) {
                    if (offhandItem.getItemMeta() instanceof CrossbowMeta meta) {
                        meta.setChargedProjectiles(null);
                        offhandItem.setItemMeta(meta);
                    } else if (offhandItem.getType().equals(Material.ARROW) || offhandItem.getType().equals(Material.COOKED_BEEF)) {
                        player.getInventory().setItemInOffHand(null);
                    }
                }
                player.setRespawnLocation(player.getLocation(), true);
            }
        for (Entity entity : arena.getBrickSpawn().getWorld().getEntities()) {
            if (!CLEARING_ENTITIES.contains(entity.getType())) continue;
            if (arena.checkLocationInbounds(entity.getLocation())) entity.remove();
        }
        spawnBrick();
        // Start countdown
        for (int i = 3; i > 0; i--) {
            final int j = i;
            new BukkitRunnable () {
                public void run () {
                    playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, 5f, 1F));
                    showTitle(Title.title(Component.text(String.valueOf(j)), Component.text(""), Title.Times.times(Duration.ZERO,Duration.ofMillis(400),Duration.ofMillis(200))));
                }
            }.runTaskLater(Brickball.getInstance(), 200 - 20L * j);
        }
        new BukkitRunnable () {
            public void run () {
                playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, 5f, 2F));
                showTitle(Title.title(Component.text("Round Start!"), Component.text(""), Title.Times.times(Duration.ZERO,Duration.ofMillis(200),Duration.ofMillis(200))));
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
                    playSound(Sound.sound(Key.key("block.glass.break"), Sound.Source.BLOCK, 30f, 2f));
                    if (objective.getScore(teamNames[i]).getScore() < settings.getInt(MatchSettings.Setting.POINTS_TO_WIN))
                        startRound();
                    else {
                        sendMessage(Component.text("[Brickball] ").append(Component.text(teamNames[i])).append(Component.text(" has won the match!")));
                        running = false;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Brickball.getInstance().getMatchManager().endMatch(BrickballMatch.this);
                            }
                        }.runTaskLater(Brickball.getInstance(), 200);
                    }
                }
    }

    public void spawnBrick() {
        Location brickSpawn = arena.getBrickSpawn();
        Item brick = (Item) brickSpawn.getWorld().spawnEntity(brickSpawn, EntityType.ITEM);
        brick.setItemStack(new ItemStack(Material.BRICK, 1));
        // STAY THERE
        brick.setVelocity(new Vector(0, 0, 0));
        brick.setGlowing(true);
    }

    public boolean joinMatch(Player player) {
        teams[teams.length-1].addPlayer(player);
        players.add(player);
        player.setScoreboard(scoreboard);
        // If it's already in progress, they need to be put into spectator mode.
        if (running) {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(arena.getBrickSpawn());
        }
        sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" joined the match.")));
        return true;
    }

    public boolean leaveMatch(Player player) {
        for (Team team : teams)
            team.removePlayer(player);
        // Don't take the brick with you when leaving
        if (player.getInventory().contains(Material.BRICK))
            spawnBrick();
        player.setGlowing(false);
        player.setUnsaturatedRegenRate(80);
        player.setSaturatedRegenRate(10);
        player.clearActivePotionEffects();
        player.getInventory().clear();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setRespawnLocation(player.getWorld().getSpawnLocation(), true);
        player.setGameMode(GameMode.SURVIVAL);
        if (player.getRespawnLocation() != null)
            player.teleport(player.getRespawnLocation());
        sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" left the match.")));
        return players.remove(player);
    }
    public boolean joinTeam(Player player, int teamID) {
        if (!players.contains(player))
            if (!joinMatch(player)) return false;
        if (teamID >= teams.length) return false;
        // No team switching once the game has started!
        if (running) return false;
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
        running = false;
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
            player.setUnsaturatedRegenRate(80);
            player.setSaturatedRegenRate(10);
            player.setGameMode(GameMode.SURVIVAL);
            player.setRespawnLocation(player.getWorld().getSpawnLocation(), true);
            if (player.getRespawnLocation() != null)
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

    public MatchSettings getSettings() {
        return settings;
    }
}

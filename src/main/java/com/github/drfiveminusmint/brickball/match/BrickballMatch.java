package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.BrickballArena;
import com.github.drfiveminusmint.brickball.scheduling.TimerUpdateHelper;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.github.drfiveminusmint.brickball.util.Counter;
import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BrickballMatch implements ForwardingAudience {
    private final int matchID;
    private Scoreboard scoreboard;
    private Objective matchScoreObjective;
    private final Team[] teams = new Team[3];
    private final BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE};
    private final String[] teamNames = {"Team 1", "Team 2", "Spectators"};
    private final HashSet<Player> players = new HashSet<>();
    private final BossBar timerBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SEGMENTED_20);
    private final BossBar shotClockBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_10);
    private final BrickballArena arena;
    private final MatchSettings settings = new MatchSettings();
    private TimerUpdateHelper timeHelper = new TimerUpdateHelper(this);

    private final Map<Player, Counter> kills = new HashMap<>(), deaths = new HashMap<>(), scores = new HashMap<>();

    private static final Set<EntityType> CLEARING_ENTITIES = Set.of(EntityType.ITEM, EntityType.ARROW);
    private int timer, timeLimit, shotClock, shotClockMax;
    private MatchState state;

    public BrickballMatch(ArenaTemplate template, Location minPoint, int matchID) {
        this.matchID = matchID;
        arena = new BrickballArena(template, minPoint, matchID);
    }

    public void initialize(int priority) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        matchScoreObjective = scoreboard.registerNewObjective("match score", Criteria.DUMMY, Component.text("Points"));
        for (int i = 0; i <= 1; i++) {
            teams[i] = scoreboard.registerNewTeam(teamNames[i]);
            teams[i].color(teamColors[i].textColor);
            teams[i].addEntry(teamNames[i]);
            teams[i].setAllowFriendlyFire(false);
            matchScoreObjective.getScore(teamNames[i]).setScore(0);
        }
        state = MatchState.PREPARING;
        teams[2] = scoreboard.registerNewTeam(teamNames[2]);
        matchScoreObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        matchScoreObjective.setAutoUpdateDisplay(true);
        shotClockBar.setVisible(false);
        arena.generateArena(priority);
    }

    public boolean startMatch () {
        if (state == MatchState.RUNNING || state == MatchState.STOPPING) return false;
        timeLimit = settings.getInt(MatchSettings.Setting.TIMER);
        shotClockMax = settings.getInt(MatchSettings.Setting.SHOT_CLOCK);
        timeHelper = new TimerUpdateHelper(this);
        try { // horrible
            timeHelper.runTaskTimer(Brickball.getInstance(), 0, 20);
        } catch (Exception ex) {}
        if (timeLimit >= 1) {
            timer = 0;
            for (Player player : players)
                timerBar.addPlayer(player);
            updateBossBar(timerBar, timer, timeLimit, "Time Remaining: %d:%02d");
        }
        if (shotClockMax >= 1) {
            for (Player player : players)
                shotClockBar.addPlayer(player);
        }
        //TODO more elegant handling
        for (int i = 0; i <= 1; i++)
            for (String string : teams[i].getEntries()) {
                Player player = Bukkit.getServer().getPlayer(string);
                if (player != null)
                    setupPlayer(player, i);
            }
        //Setup spectators
        for (String string : teams[teams.length-1].getEntries()) {
            Player player = Bukkit.getServer().getPlayer(string);
            if (player == null) continue;
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(arena.getBrickSpawn());
        }
        state = MatchState.RUNNING;
        startRound();
        return true;
    }

    public void startRound() {
        arena.closeDoors();
        shotClockMax = settings.getInt(MatchSettings.Setting.SHOT_CLOCK);
        shotClockBar.setVisible(false);
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
                // Full heal and clear all status
                player.heal(20);
                player.setFoodLevel(20);
                player.setSaturation(settings.getInt(MatchSettings.Setting.SATURATION));
                player.setFireTicks(0);
                player.clearActivePotionEffects();
                // Give steaks and arrows
                if (settings.getInt(MatchSettings.Setting.STEAKS) > 0) {
                    player.getInventory().remove(Material.COOKED_BEEF);
                    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, settings.getInt(MatchSettings.Setting.STEAKS)));
                }
                if (settings.getInt(MatchSettings.Setting.ARROWS) > 0) {
                    player.getInventory().remove(Material.ARROW);
                    player.getInventory().addItem(new ItemStack(Material.ARROW, settings.getInt(MatchSettings.Setting.ARROWS)));
                }
                // Remove any charged arrows from the crossbow
                for (ItemStack itemStack : player.getInventory()) {
                    if (itemStack == null) continue;
                    if (itemStack.getItemMeta() instanceof CrossbowMeta meta) {
                        meta.setChargedProjectiles(null);
                        itemStack.setItemMeta(meta);
                    }
                }
                //prevent item smuggling
                ItemStack offhandItem = player.getInventory().getItemInOffHand();
                if (offhandItem.getItemMeta() instanceof CrossbowMeta meta) {
                    meta.setChargedProjectiles(null);
                    offhandItem.setItemMeta(meta);
                } else if (offhandItem.getType().equals(Material.ARROW) || offhandItem.getType().equals(Material.COOKED_BEEF)) {
                    player.getInventory().setItemInOffHand(null);
                }
                player.setRespawnLocation(player.getLocation(), true);
            }
        removeGroundEntities();
        spawnBrick();
        // Start countdown
        for (int i = 3; i > 0; i--) {
            final int j = i;
            new BukkitRunnable () {
                public void run () {
                    playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, 5f, 1F));
                    showTitle(Title.title(Component.text(String.valueOf(j)), Component.text(""), Title.Times.times(Duration.ZERO,Duration.ofMillis(400),Duration.ofMillis(200))));
                }
            }.runTaskLater(Brickball.getInstance(), 160 - 20L * j);
        }
        new BukkitRunnable () {
            public void run () {
                playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, 5f, 2F));
                showTitle(Title.title(Component.text("Round Start!"), Component.text(""), Title.Times.times(Duration.ZERO,Duration.ofMillis(200),Duration.ofMillis(200))));
                arena.openDoors();
            }
        }.runTaskLater(Brickball.getInstance(), 160);
    }

    public void checkScoring(Player player) {
        for (int i = 0; i <= 1; i++)
            if (teams[i].hasPlayer(player))
                if (arena.checkLocationInSpawn(player.getLocation(), 1-i)) {
                    matchScoreObjective.getScore(teamNames[i]).setScore(matchScoreObjective.getScore(teamNames[i]).getScore() + 1);
                    player.getInventory().remove(Material.BRICK);
                    player.setGlowing(false);
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                    playSound(Sound.sound(Key.key("block.glass.break"), Sound.Source.BLOCK, 30f, 2f));
                    stopShotClock();
                    reportScore(player);
                    if (matchScoreObjective.getScore(teamNames[i]).getScore() < settings.getInt(MatchSettings.Setting.POINTS_TO_WIN))
                        startRound();
                    else {
                        sendMessage(Component.text("[Brickball] ").color(NamedTextColor.GOLD).append(Component.text(teamNames[i]).color(teamColors[i].textColor)).append(Component.text(" has won the match!").color(NamedTextColor.GOLD)));
                        displayStats(players);
                        pause();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Brickball.getInstance().getMatchManager().endMatch((BrickballMatch.this));
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
        if(brickSpawn.getBlock().getRelative(BlockFace.DOWN).getType().isAir()) // Make sure the brick cannot fall
            brickSpawn.getBlock().getRelative(BlockFace.DOWN).setType(Material.STONE);
        brick.setGlowing(true);
    }

    public boolean joinMatch(Player player) {
        teams[teams.length-1].addPlayer(player);
        players.add(player);
        player.setScoreboard(scoreboard);
        // If it's already in progress, they need to be put into spectator mode.
        if (state == MatchState.RUNNING) {
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(arena.getBrickSpawn());
            if (settings.getInt(MatchSettings.Setting.TIMER) != -1)
                timerBar.addPlayer(player);
            if (settings.getInt(MatchSettings.Setting.SHOT_CLOCK) != -1)
                shotClockBar.addPlayer(player);
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
        cleanupPlayer(player);
        sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" left the match.")));
        return players.remove(player);
    }
    public boolean joinTeam(Player player, int teamID) {
        if (!players.contains(player))
            if (!joinMatch(player)) return false;
        if (teamID >= teams.length) return false;
        // No team switching while the game is actively running!
        if (state == MatchState.RUNNING) return false;
        // Remove the player from any current teams
        for(Team team : teams)
            team.removePlayer(player);
        teams[teamID].addPlayer(player);
        if (state == MatchState.PAUSED)
            setupPlayer(player, teamID);
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
        state = MatchState.STOPPING;
        arena.cleanupArena();
        removeGroundEntities();
        sendMessage(Component.text("[Debug] Shutdown Successful"));
        for (Player player : players) {
            for (Team team : teams)
                team.removePlayer(player);
            cleanupPlayer(player);
        }
        for (Team team : teams) {
            Set<String> entries = team.getEntries();
            team.removeEntries(entries);
            team.unregister();
        }
        players.clear();
        // Prevent a resource leak
        try {
            timeHelper.cancel();
        } catch (Exception ex) {}
        timeHelper = null;
    }

    public void freeze() {
        state = MatchState.STOPPING;
        // remove all players
        for (Player player : players) {
            for (Team team : teams)
                team.removePlayer(player);
            cleanupPlayer(player);
        }
        players.clear();
        // reset scores
        matchScoreObjective.getScore(teamNames[0]).setScore(0);
        matchScoreObjective.getScore(teamNames[1]).setScore(0);
        // remove items
        removeGroundEntities();
        // clear stat tracking
        kills.clear(); scores.clear(); deaths.clear();
        try { // Horrible but bukkit scheduling loves to chuck random exceptions at me here
            timeHelper.cancel();
        } catch (Exception ex) {}
        state = MatchState.FROZEN;
        timeHelper = null;
    }

    public void unfreeze() {
        if (state != MatchState.FROZEN) return;
        state = MatchState.PREPARING;
    }

    @Nullable
    public Team getPlayerTeam (Player player) {
        for(Team team : teams)
            if (team.hasPlayer(player)) return team;
        return null;
    }

    public void reportDeath(Player dead) {
        if (!deaths.containsKey(dead)) deaths.put(dead, new Counter());
        deaths.get(dead).increment();
        if (dead.getKiller() != null) {
            if (!kills.containsKey(dead.getKiller())) kills.put(dead.getKiller(), new Counter());
            kills.get(dead.getKiller()).increment();
        }
    }

    public void reportScore(Player scorer) {
        if (!scores.containsKey(scorer)) scores.put(scorer, new Counter());
        scores.get(scorer).increment();
    }

    private void setupPlayer(Player player, int team) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        // setup armor
        inventory.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        inventory.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        LeatherArmorMeta meta = (LeatherArmorMeta) inventory.getBoots().getItemMeta();
        meta.setColor(teamColors[team].bukkitColor);
        meta.setUnbreakable(true);
        for (ItemStack armorPiece : inventory.getArmorContents()) {
            armorPiece.setItemMeta(meta);
        }
        // setup weapons
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true);
        sword.setItemMeta(swordMeta);
        inventory.addItem(sword);
        inventory.addItem(new ItemStack(Material.CROSSBOW));
        player.updateInventory(); // this needs to run for the inventory changes to apply
    }
    private void cleanupPlayer(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.setGlowing(false);
        player.setUnsaturatedRegenRate(80);
        player.setSaturatedRegenRate(10);
        player.setGameMode(GameMode.SURVIVAL);
        player.setRespawnLocation(player.getWorld().getSpawnLocation(), true);
        timerBar.removePlayer(player);
        shotClockBar.removePlayer(player);
        if (player.getRespawnLocation() != null)
            player.teleport(player.getRespawnLocation());
        player.getInventory().clear();
        player.clearActivePotionEffects();
    }

    private void removeGroundEntities() {
        for (Entity entity : arena.getBrickSpawn().getWorld().getEntities()) {
            if (!CLEARING_ENTITIES.contains(entity.getType())) continue;
            if (arena.checkLocationInbounds(entity.getLocation())) entity.remove();
        }
    }

    public void displayStats(Set<? extends Audience> viewers) {
        for (Audience audience : viewers) {
            audience.sendMessage(Component.text("Kills:").color(NamedTextColor.GOLD));
            for (Player player : kills.keySet())
                audience.sendMessage(player.displayName().color(teamColors[(scoreboard.getPlayerTeam(player) != null && scoreboard.getPlayerTeam(player).equals(teams[0])) ? 0 : 1].textColor)
                        .append(Component.text(": ").color(NamedTextColor.WHITE))
                        .append(Component.text(kills.get(player).value()).color(NamedTextColor.WHITE)));
            audience.sendMessage(Component.text("Deaths:").color(NamedTextColor.GOLD));
            for (Player player : deaths.keySet())
                audience.sendMessage(player.displayName().color(teamColors[(scoreboard.getPlayerTeam(player) != null && scoreboard.getPlayerTeam(player).equals(teams[0])) ? 0 : 1].textColor)
                        .append(Component.text(": ").color(NamedTextColor.WHITE))
                        .append(Component.text(deaths.get(player).value())).color(NamedTextColor.WHITE));
            audience.sendMessage(Component.text("Points Scored:").color(NamedTextColor.GOLD));
            for (Player player : scores.keySet())
                audience.sendMessage(player.displayName().color(teamColors[(scoreboard.getPlayerTeam(player) != null && scoreboard.getPlayerTeam(player).equals(teams[0])) ? 0 : 1].textColor)
                        .append(Component.text(": ").color(NamedTextColor.WHITE))
                        .append(Component.text(scores.get(player).value()).color(NamedTextColor.WHITE)));
        }
    }

    private void updateBossBar(BossBar bar, int currentTime, int maxTime, String titleFormat) {
        bar.setTitle(String.format(titleFormat, ((maxTime-currentTime) / 60), (maxTime-currentTime) % 60));
        bar.setProgress((maxTime-currentTime) / (maxTime * 1.0));
    }

    public void tickTimer() {
        // shotClockMax = 0 indicates the shot clock is disabled, shotClock = -1 indicates it is not running
        if ((shotClockMax > 0) && (shotClock != -1) && state.equals(MatchState.RUNNING)) {
            // Shotclock is enabled and active
            shotClockBar.setVisible(true);
            updateBossBar(shotClockBar, shotClockMax - (--shotClock), shotClockMax, "Shot Clock: %d:%02d");
            if (shotClock == 0) {
                for(Player player : players)
                    if (player.getInventory().contains(Material.BRICK) || player.getInventory().getItemInOffHand().getType().equals(Material.BRICK)) {
                        // Shot clock has expired
                        // Remove the brick from the offending team
                        player.getInventory().remove(Material.BRICK);
                        if (player.getInventory().getItemInOffHand().getType().equals(Material.BRICK))
                            player.getInventory().setItemInOffHand(null);
                        player.updateInventory();
                        // give the brick to the other team
                        turnover(player);
                    }
                stopShotClock();
            } else if (shotClock == 20) {
                // Warn the players
                playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, 5f, 1F));
                sendMessage(Component.text("[Brickball] The BRICK grows impatient! Attempt to score within the next 20 seconds! Or else...").color(NamedTextColor.DARK_RED));
            } else if (shotClock <= 5) {
                playSound(Sound.sound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, 5f, 1F));
            }
        } else {
            // don't display shot clock bar if invisible
            shotClockBar.setVisible(false);
        }
        if (timer < timeLimit) {
            timer++;
            updateBossBar(timerBar, timer, timeLimit, "Time Remaining: %d:%02d");
        } else if (timeLimit > 0) { // end the match if the match timer is enabled and time is over the limit
            int winningTeam = 0;
            if (matchScoreObjective.getScore(teamNames[0]).getScore() == matchScoreObjective.getScore(teamNames[1]).getScore())
                sendMessage(Component.text("[Brickball] The match ended in a tie!").color(NamedTextColor.GOLD));
            else {
                winningTeam = (matchScoreObjective.getScore(teamNames[0]).getScore() > matchScoreObjective.getScore(teamNames[1]).getScore()) ? 0 : 1;
                sendMessage(Component.text("[Brickball] ").color(NamedTextColor.GOLD).append(Component.text(teamNames[winningTeam]).color(teamColors[winningTeam].textColor)).append(Component.text(" has won the match!").color(NamedTextColor.GOLD)));
            }
            displayStats(players);
            pause();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Brickball.getInstance().getMatchManager().endMatch((BrickballMatch.this));
                }
            }.runTaskLater(Brickball.getInstance(), 200);
        }
    }

    // Signal to the match that the target player has turned the ball over
    public void turnover(Player losingPlayer) {
        int reboundTeam = (teams[0].hasPlayer(losingPlayer)) ? 1 : 0;
        // Notify players
        playSound(Sound.sound(Key.key("entity.blaze.death"), Sound.Source.BLOCK, 5f, 1F));
        sendMessage(Component.text("[Brickball] ").color(NamedTextColor.DARK_RED)
                .append(Component.text(teamNames[1-reboundTeam]).color(teamColors[1-reboundTeam].textColor))
                .append(Component.text(" has displeased the BRICK. It transfers itself to ").color(NamedTextColor.DARK_RED))
                .append(Component.text(teamNames[reboundTeam]).color(teamColors[reboundTeam].textColor))
                .append(Component.text("...").color(NamedTextColor.DARK_RED))
        );
        // ensure the player's respawn location isn't accidentally reset
        Location savedRespawnLocation = losingPlayer.getRespawnLocation();
        // Reset the round
        stopShotClock();
        startRound();
        losingPlayer.setRespawnLocation(savedRespawnLocation, true);
        losingPlayer.teleport(savedRespawnLocation);
        // Override brick spawn
        removeGroundEntities();
        Item brick = (Item) arena.getSpawnLocation(reboundTeam).getWorld().spawnEntity(arena.getSpawnLocation(reboundTeam), EntityType.ITEM);
        brick.setItemStack(new ItemStack(Material.BRICK, 1));
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
    public MatchState getState() {return state;}
    public String getMapID() {return arena.getTemplateID();}

    public void pause() {state = MatchState.PAUSED;}
    public void startShotClock() {
        if (shotClockMax <= 0) return;
        shotClock = shotClockMax;
        updateBossBar(shotClockBar, 0, shotClockMax, "Shot Clock: %d:%02d");
        shotClockBar.setVisible(true);
    }
    public void stopShotClock() {shotClock = -1;}
}

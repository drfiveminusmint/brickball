package com.github.drfiveminusmint.brickball.stats;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.match.MatchResult;
import com.github.drfiveminusmint.brickball.scheduling.SaveStatsTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class FormatStats {

    // Constant values for rating algorithm
    private static final int WIN_BONUS = 5;
    private static final int POINT_BONUS = 5;
    private static final int RATING_DIFFERENCE_PER_BONUS = 20;
    private final HashMap<UUID, PlayerStats> perPlayerStats = new HashMap<>();
    private final BrickballFormat format;

    public FormatStats(BrickballFormat format) {this.format = format;}


    public int getPlayerStat(OfflinePlayer player, TrackedStat stat) {
        // null results are ok here
        PlayerStats playerStats = perPlayerStats.get(player.getUniqueId());
        if (playerStats == null) return -1;
        return playerStats.get(stat);
    }

    public synchronized boolean updateStats (MatchResult result) {
        if (format.getIsRated()) {
            // update ratings
            // get average rating
            int winnerRating = 0, loserRating = 0;
            for (Player player : result.winningTeam)
                winnerRating += getPlayerStatsSafe(player.getUniqueId()).get(TrackedStat.RATING);
            winnerRating /= result.winningTeam.size();
            if (!result.losingTeam.isEmpty()) {
                for (Player player : result.losingTeam)
                    loserRating += getPlayerStatsSafe(player.getUniqueId()).get(TrackedStat.RATING);
                loserRating /= result.losingTeam.size();
            } else {
                // fallback 1: use leavers as the losing team
                for (Player player : result.leavers)
                    loserRating += getPlayerStatsSafe(player.getUniqueId()).get(TrackedStat.RATING);
                if (result.leavers.isEmpty())
                    // fallback 2 - if this happens, something has gone terribly wrong, but we still don't want a crash
                    loserRating = winnerRating;
                else
                    loserRating /= result.leavers.size();
            }

            // calculate points gain/loss
            // bonus for winning
            int delta = result.winningScore == result.losingScore ? 0 : WIN_BONUS;
            // point bonus
            delta += (result.winningScore - result.losingScore) * POINT_BONUS;
            // rating difference bonus/malus
            delta += (loserRating-winnerRating) / RATING_DIFFERENCE_PER_BONUS;

            // distribute rating gain/loss
            int winningPoints = delta * result.winningTeam.size();
            int losingPoints = -winningPoints;
            if (!result.leavers.isEmpty()) {
                // leavers absorb half the total losses
                if (losingPoints < 0) {
                    if (result.losingTeam.isEmpty()) {
                        distributePoints(result.leavers, losingPoints);
                    } else {
                        distributePoints(result.leavers, losingPoints/2);
                        losingPoints = (losingPoints/2) + (losingPoints%2);
                    }
                } else {
                    distributePoints(result.leavers, winningPoints/2);
                    winningPoints = (winningPoints/2) + (winningPoints%2);
                }
            }
            distributePoints(result.winningTeam, winningPoints);
            distributePoints(result.losingTeam, losingPoints);
        }
        // sum up other stats
        for (Player player : result.winningTeam) {
            PlayerStats stats = getPlayerStatsSafe(player.getUniqueId());
            if (result.winningScore != result.losingScore)
                stats.adjust(TrackedStat.WINS, 1);
            else
                stats.adjust(TrackedStat.DRAWS, 1);
            stats.adjust(TrackedStat.ROUND_WINS, result.winningScore);
            stats.adjust(TrackedStat.ROUND_LOSSES, result.losingScore);
        }
        for (Player player : result.losingTeam) {
            PlayerStats stats = getPlayerStatsSafe(player.getUniqueId());
            if (result.winningScore != result.losingScore)
                stats.adjust(TrackedStat.LOSSES, 1);
            else
                stats.adjust(TrackedStat.DRAWS, 1);
            stats.adjust(TrackedStat.ROUND_WINS, result.losingScore);
            stats.adjust(TrackedStat.ROUND_LOSSES, result.winningScore);
        }
        // leavers always lose
        for (Player player : result.leavers)
            getPlayerStatsSafe(player.getUniqueId()).adjust(TrackedStat.LOSSES, 1);
        // sum up kills, deaths, points
        for (Player player : result.points.keySet())
            getPlayerStatsSafe(player.getUniqueId()).adjust(TrackedStat.POINTS, result.points.get(player).value());
        for (Player player : result.kills.keySet())
            getPlayerStatsSafe(player.getUniqueId()).adjust(TrackedStat.KILLS, result.kills.get(player).value());
        for (Player player : result.deaths.keySet())
            getPlayerStatsSafe(player.getUniqueId()).adjust(TrackedStat.DEATHS, result.deaths.get(player).value());

        // Save stats to file
        Brickball.getInstance().getScheduler().submitTask(new SaveStatsTask(-1, this, new File(Brickball.getStatsFolder(), format.getName() + ".csv")));
        return true;
    }

    public void displayStats(Player requester, OfflinePlayer tracked) {
        requester.sendMessage(Component.text("[Brickball] Lifetime stats for ", NamedTextColor.GOLD)
                .append(Component.text(tracked.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" in format ", NamedTextColor.GOLD))
                .append(Component.text(format.getName(), NamedTextColor.AQUA)));
        for (TrackedStat stat : TrackedStat.values())
            requester.sendMessage(String.format("%s: %d", stat.name(), getPlayerStatsSafe(tracked.getUniqueId()).get(stat)));
    }

    // Gets a player's stat sheet if present, and creates one for them if absent.
    private @NotNull PlayerStats getPlayerStatsSafe(UUID uuid) {
        if (perPlayerStats.containsKey(uuid)) return perPlayerStats.get(uuid);
        PlayerStats newStats = new PlayerStats();
        perPlayerStats.put(uuid, newStats);
        return newStats;
    }


    private static class PlayerStats {
        private final ConcurrentHashMap<TrackedStat, Integer> backing = new ConcurrentHashMap<>();

        public PlayerStats() {
            for (TrackedStat ts : TrackedStat.values())
                backing.put(ts, 0);
            backing.put(TrackedStat.RATING, 1000);
        }

        public int get(TrackedStat stat) {
            return backing.getOrDefault(stat, 0);
        }

        public synchronized void adjust(TrackedStat stat, int value) {
            backing.put(stat, backing.getOrDefault(stat, 0) + value);
        }
    }

    private void distributePoints(Collection<Player> players, int points) {
        if (players.isEmpty()) // this HOPEFULLY should never happen
            return;
        int remainder = (points % players.size());
        int quotient = (points / players.size());
        for (Player player : players) {
            PlayerStats stats = getPlayerStatsSafe(player.getUniqueId());
            int old = stats.get(TrackedStat.RATING);
            if (remainder-- > 0)
                stats.adjust(TrackedStat.RATING, quotient+1);
            else
                stats.adjust(TrackedStat.RATING, quotient);
            player.sendMessage(Component.text("[Brickball] Rating Updated: ", NamedTextColor.GOLD)
                    .append(Component.text(String.format("%d -> %d", old, stats.get(TrackedStat.RATING)), NamedTextColor.YELLOW)));
        }
    }

    // Read in the format stats from a file
    // NEVER EVER call this from the main thread, use an IOTask
    public synchronized void readFromFile(File f) {
        try (FileReader fileReader = new FileReader(f)) {
            // skip our first header line
            BufferedReader reader = new BufferedReader(fileReader);
            reader.readLine();

            String nextLine = reader.readLine();
            while (nextLine != null) {
                String[] entries = nextLine.split(",");
                PlayerStats playerStats = new PlayerStats();
                perPlayerStats.put(UUID.fromString(entries[0]), playerStats);
                // skip the "name" column
                // then read in the values in the order they're defined in the enum
                for (int i = 2; i < entries.length && i < TrackedStat.values().length + 2; i++)
                    try {
                        playerStats.backing.put(TrackedStat.values()[i-2], Integer.parseInt(entries[i]));
                    } catch (NumberFormatException exception) {
                        Brickball.getInstance().getLogger().log(Level.SEVERE, String.format("Error reading stats in file %s - cannot parse %s of player with UUID %s",
                                f.getName(), TrackedStat.values()[i-2].name(), entries[0]));
                    }
                nextLine = reader.readLine();
            }

            reader.close();
        } catch (FileNotFoundException exception) {
            Brickball.getInstance().getLogger().log(Level.WARNING, "No stat sheet found for format " + f.getName());
        } catch (IOException exception) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Something went wrong loading " + f.getName());
        }
    }

    // Read in the format stats from a file
    // NEVER EVER call this from the main thread, use an IOTask
    public synchronized void writeToFile(File f) {
        try {
            if (f.exists()) f.delete();
            f.createNewFile();
            PrintWriter writer = new PrintWriter(f);

            // Build header
            String[] nextLine = new String[TrackedStat.values().length + 2];
            nextLine[0] = "UUID";
            nextLine[1] = "Name"; // This column is just to make our CSV more human-readable
            {
                int i = 2;
                for (TrackedStat stat : TrackedStat.values())
                    nextLine[i++] = stat.name().toLowerCase().translateEscapes();
                writer.println(String.join(",", nextLine));
            }

            // Export data
            for (UUID uuid : perPlayerStats.keySet()) {
                nextLine[0] = uuid.toString();
                PlayerStats stats = perPlayerStats.get(uuid);
                nextLine[1] = Bukkit.getOfflinePlayer(uuid).getName();
                int i = 2;
                for (TrackedStat stat : TrackedStat.values())
                    nextLine[i++] = Integer.toString(stats.get(stat));
                writer.println(String.join(",", nextLine));
            }

            // Close and save file
            writer.close();
        } catch (IOException exception) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error: something went wrong writing to " + f.getName());
        }
    }

    public enum TrackedStat {
        RATING, WINS, LOSSES, DRAWS, ROUND_WINS, ROUND_LOSSES, POINTS, KILLS, DEATHS
    }
}

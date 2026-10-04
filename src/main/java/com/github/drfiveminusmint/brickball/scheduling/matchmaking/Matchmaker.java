package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.lobby.Lobby;
import com.github.drfiveminusmint.brickball.stats.FormatStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;

public class Matchmaker extends BukkitRunnable {
    private static final double HEURISTIC_TIME_COEFFICIENT = 5.0, HEURISTIC_TIME_POWER = 0.25;
    private static final int HEURISTIC_MAX_TIME_BONUS = 100, HEURISTIC_UNFILLED_BONUS = 100;
    private static final int MINIMUM_SCORE = 300;

    private final PriorityBlockingQueue<QueueingPlayer> queue = new PriorityBlockingQueue<>();
    private final BrickballFormat format;

    public Matchmaker(BrickballFormat format) {
        this.format = format;
    }

    @Override
    public void run() {
        // don't bother if there aren't enough players to create a match
        if (queue.size() < format.getMinPlayersPerTeam() * 2 || queue.isEmpty())
            return;
        Brickball.getInstance().getLogger().log(Level.INFO, "Attempting to matchmake for format " + format.getName());
        // Prioritize finding a match for the player who's been queueing the longest
        // If absolutely no matches can be found for them, proceed to the next player in the queue.
        HashSet<QueueingPlayer> removed = new HashSet<>(queue.size()/2 + 1);
        while (!queue.isEmpty()) {
            HashSet<MatchCandidate> candidates = new HashSet<>();
            MatchCandidate firstCandidate = new MatchCandidate();
            QueueingPlayer firstPlayer = queue.poll();
            // store the players we've set aside
            removed.add(firstPlayer);
            firstCandidate.addPlayer(firstPlayer);
            // add the base of our tree
            candidates.add(firstCandidate);

            // build out the tree by adding players to existing candidates
            HashSet<QueueingPlayer> processed = new HashSet<>(queue.size());
            while (!queue.isEmpty()) {
                Brickball.getInstance().getLogger().log(Level.INFO, "Tree Building started");
                QueueingPlayer player = queue.poll();
                processed.add(player);
                HashSet<MatchCandidate> newCandidates = new HashSet<>();
                for (MatchCandidate candidate : candidates)
                    if (evaluateAddition(candidate, player) > 0) {
                        Brickball.getInstance().getLogger().log(Level.INFO, "Adding match candidate");
                        MatchCandidate clone = candidate.clone();
                        clone.addPlayer(player);
                        newCandidates.add(clone);
                    }
                if (!newCandidates.isEmpty())
                    Brickball.getInstance().getLogger().log(Level.WARNING, "New candidates found");
                candidates.addAll(newCandidates);
            }
            // We now have a set of candidates to create teams from
            // For each candidate that has the right number of players, create the teams and score them
            // Find the best scoring match
            PreliminaryTeams teams = null;
            int teamsScore = MINIMUM_SCORE;
            for (MatchCandidate candidate : candidates) {
                Brickball.getInstance().getLogger().log(Level.INFO, "Match candidate found! " + players2String(candidate.getPlayers()));
                if (candidate.getPlayers().size() >= 2 * format.getMinPlayersPerTeam()) {
                    PreliminaryTeams replacement = new PreliminaryTeams(candidate, format);
                    int score = scoreTeams(replacement);
                    Brickball.getInstance().getLogger().log(Level.INFO, "Built teams: " + players2String(replacement.getTeam1())+ players2String(replacement.getTeam2()));
                    Brickball.getInstance().getLogger().log(Level.INFO, "Teams scored " + score);
                    if (score > teamsScore) {
                        Brickball.getInstance().getLogger().log(Level.INFO, "New high score!");
                        teamsScore = score;
                        teams = replacement;
                    }
                }
            }
            if (teams != null) {
                // restore the queue
                queue.addAll(processed);
                queue.addAll(removed);
                // prevent a race condition here by running this synchronously
                final PreliminaryTeams finalTeams = teams;
                Bukkit.getScheduler().runTask(Brickball.getInstance(), () -> {
                    // run sanity check
                    boolean sanityCheck = true;
                    for (QueueingPlayer player : finalTeams.getTeam1())
                        if (Brickball.getInstance().getLobbyList().getLobbyByPlayer(player.getPlayer()) != null) {
                            // this player is already in a lobby
                            queue.remove(player);
                            sanityCheck = false;
                        }
                    for(QueueingPlayer player : finalTeams.getTeam2())
                        if (Brickball.getInstance().getLobbyList().getLobbyByPlayer(player.getPlayer()) != null) {
                            // this player is already in a lobby
                            queue.remove(player);
                            sanityCheck = false;
                        }
                    // if any players are already in a lobby, reject this match
                    if (!sanityCheck) return;
                    // Match accepted, remove all players from all queues
                    for (QueueingPlayer player : finalTeams.getTeam1())
                        Brickball.getInstance().endPlayerQueue(player.getPlayer());
                    for (QueueingPlayer player : finalTeams.getTeam2())
                        Brickball.getInstance().endPlayerQueue(player.getPlayer());
                    // create the lobby and place players within
                    Lobby lobby = new Lobby(format, false);
                    for (QueueingPlayer player : finalTeams.getTeam1()) {
                        lobby.join(player.getPlayer(), 0);
                    }
                    for (QueueingPlayer player : finalTeams.getTeam2()) {
                        lobby.join(player.getPlayer(), 1);
                    }
                });
                return;
            }
            // return processed players to the queue and start again
            queue.addAll(processed);
        }
        // We've failed to find any matches, add all players to the queue and try again next cycle.
        queue.addAll(removed);
    }

    public void addPlayer(Player player) {
        int rating = Brickball.getInstance().getFormatStats(format).getPlayerStat(player, FormatStats.TrackedStat.RATING);
        if (rating <= 0)
            rating = 1000;
        queue.add(new QueueingPlayer(player, System.currentTimeMillis(),
                rating));
    }

    public boolean hasPlayer (Player player) {
        for (QueueingPlayer other : queue) {
            if (other.getPlayer() == player)
                return true;
        }
        return false;
    }

    public boolean removePlayer (Player player) {
        for (QueueingPlayer other : queue) {
            if (other.getPlayer() == player) {
                return queue.remove(other);
            }
        }
        return false;
    }

    private int evaluateAddition(MatchCandidate candidate, QueueingPlayer player) {
        Brickball.getInstance().getLogger().log(Level.INFO, "Evaluating player addition: " + player.getPlayer().getName());
        int size = candidate.getPlayers().size();
        if (size >= format.getMaxPlayersPerTeam() * 2)
            return -1; // never allow this
        // Score whether this addition is likely to make the match better or worse
        long time = System.currentTimeMillis();
        int ratingDifferencePenalty = Math.min( Math.min(candidate.getHighRating() - player.getRating(), 0),
                                                Math.min(player.getRating() - candidate.getLowRating(), 0));
        int queueTimeBonus = Math.max((int) (HEURISTIC_TIME_COEFFICIENT * Math.pow(time - player.getJoinTime(), HEURISTIC_TIME_POWER)), HEURISTIC_MAX_TIME_BONUS);
        int unfilledBonus = (size < format.getMinPlayersPerTeam()*2) ? HEURISTIC_UNFILLED_BONUS : 0;
        return ratingDifferencePenalty + queueTimeBonus + unfilledBonus;
    }

    private int scoreTeams(PreliminaryTeams teams) {
        // Get data
        int totalPlayers = teams.getTeam1().size() + teams.getTeam2().size();
        int sumRatingSquared = 0;
        long totalWait = 0;
        long time = System.currentTimeMillis();
        for (QueueingPlayer player : teams.getTeam1()) {
            sumRatingSquared += (int) Math.pow(player.getRating(), 2);
            totalWait += time - player.getJoinTime();
        }
        float meanRating = (teams.getTeam1Rating() + teams.getTeam2Rating()+ 0f) / totalPlayers;
        Brickball.getInstance().getLogger().log(Level.INFO, "meanRating = " + meanRating);
        double mqs = 10000f /
                (Math.sqrt(sumRatingSquared - totalPlayers * Math.pow(meanRating,2))
                + Math.pow(Math.abs(teams.getTeam1Rating() - teams.getTeam2Rating()), 1.2)
                + 100);
        Brickball.getInstance().getLogger().log(Level.INFO, "MQS = " + mqs);
        int finalScore = (int) (mqs * Math.pow((1.0d * totalWait) / totalPlayers, 0.25));
        Brickball.getInstance().getLogger().log(Level.INFO, "Wait Score = ", Math.pow((1.0d * totalWait) / totalPlayers, 0.25));
        if (teams.getTeam1().size() != teams.getTeam2().size())
            finalScore -= 100; // Penalty for different sized teams
        return finalScore;
    }

    private String players2String(Collection<QueueingPlayer> players) {
        StringBuilder builder = new StringBuilder("[");
        for (QueueingPlayer player : players) {
            builder.append(player.getPlayer().getName());
            builder.append(" ");
        }
        builder.append("]");
        return builder.toString();
    }
}

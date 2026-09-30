package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.lobby.Lobby;
import com.github.drfiveminusmint.brickball.stats.FormatStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.concurrent.PriorityBlockingQueue;

public class Matchmaker extends BukkitRunnable {
    private static final double CONST_A = 1.0, CONST_B = 1.0;
    private static final int CONST_C = 100, CONST_D = 100;
    private static final int MINIMUM_SCORE = 300;

    private final PriorityBlockingQueue<QueueingPlayer> queue = new PriorityBlockingQueue<>();
    private final BrickballFormat format;

    public Matchmaker(BrickballFormat format) {
        this.format = format;
    }

    @Override
    public void run() {
        // Prioritize finding a match for the player who's been queueing the longest
        // If absolutely no matches can be found for them, proceed to the next player in the queue.
        HashSet<QueueingPlayer> removed = new HashSet<>(queue.size()/2 + 1);
        while (!queue.isEmpty()) {
            HashSet<MatchCandidate> candidates = new HashSet<>();
            MatchCandidate firstCandidate = new MatchCandidate();
            QueueingPlayer firstPlayer = queue.poll();
            removed.add(firstPlayer);
            firstCandidate.addPlayer(firstPlayer);
            // add the base of our tree
            candidates.add(firstCandidate);

            // build out the tree by adding players to existing candidates
            HashSet<QueueingPlayer> processed = new HashSet<>(queue.size());
            while (!queue.isEmpty()) {
                QueueingPlayer player = queue.poll();
                processed.add(player);
                HashSet<MatchCandidate> newCandidates = new HashSet<>();
                for (MatchCandidate candidate : candidates)
                    if (evaluateAddition(candidate, player) > 0) {
                        MatchCandidate clone = candidate.clone();
                        clone.addPlayer(player);
                        newCandidates.add(clone);
                    }
                candidates.addAll(newCandidates);
            }
            // We now have a set of candidates to create teams from
            // For each candidate that has the right number of players, create the teams and score them
            // Find the best scoring match
            PreliminaryTeams teams = null;
            int teamsScore = MINIMUM_SCORE;
            for (MatchCandidate candidate : candidates) {
                if (candidate.getPlayers().size() > 2 * format.getMinPlayersPerTeam()) {
                    PreliminaryTeams replacement = new PreliminaryTeams(candidate, format);
                    int score = scoreTeams(replacement);
                    if (score > teamsScore) {
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
        // We've failed to find any matches, add all players to the queue and try again next tick.
        queue.addAll(removed);
    }

    public void addPlayer(Player player) {
        queue.add(new QueueingPlayer(player, System.currentTimeMillis(),
                Brickball.getInstance().getFormatStats(format).getPlayerStat(player, FormatStats.TrackedStat.RATING)));
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
                return queue.remove(player);
            }
        }
        return false;
    }

    private int evaluateAddition(MatchCandidate candidate, QueueingPlayer player) {
        int size = candidate.getPlayers().size();
        if (size >= format.getMaxPlayersPerTeam())
            return -1; // never allow this
        // Score whether this addition is likely to make the match better or worse
        long time = System.currentTimeMillis();
        int ratingDifferencePenalty = Math.min( Math.min(candidate.getHighRating() - player.getRating(), 0),
                                                Math.min(player.getRating() - candidate.getLowRating(), 0));
        int queueTimeBonus = Math.max((int) (CONST_A * Math.pow(player.getJoinTime() - time, CONST_B)), CONST_C);
        int unfilledBonus = (size < format.getMinPlayersPerTeam()*2) ? CONST_D : 0;
        return ratingDifferencePenalty + queueTimeBonus + unfilledBonus;
    }

    private int scoreTeams(PreliminaryTeams teams) {
        // Get data
        int totalPlayers = teams.getTeam1().size() + teams.getTeam2().size();
        int sumRatingSquared = 0;
        long totalWait = 0;
        long time = System.currentTimeMillis();
        for (QueueingPlayer player : teams.getTeam1()) {
            sumRatingSquared += Math.pow(player.getRating(), 2);
            totalWait += time - player.getJoinTime();
        }
        float meanRating = (teams.getTeam1Rating() + teams.getTeam2Rating()+ 0f) / totalPlayers;
        double mqs = 10000f /
                (Math.sqrt(sumRatingSquared - totalPlayers * Math.pow(meanRating,2))
                + Math.pow(Math.abs(teams.getTeam1Rating() - teams.getTeam2Rating()), 1.2)
                + 100);
        int finalScore = (int) (mqs * Math.pow(totalWait / totalPlayers, 0.25));
        if (teams.getTeam1().size() != teams.getTeam2().size())
            finalScore -= 100; // Penalty for different sized teams
        return finalScore;
    }
}

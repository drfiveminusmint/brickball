package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;

public class PreliminaryTeams {
    private LinkedList<QueueingPlayer> team1 = new LinkedList<>(), team2 = new LinkedList<>();
    private int team1Rating = 0, team2Rating = 0;

    public PreliminaryTeams(MatchCandidate candidate, BrickballFormat format) {

        HashSet<QueueingPlayer> playerSet = (HashSet<QueueingPlayer>) candidate.getPlayers().clone();
        while (!playerSet.isEmpty()) {
            QueueingPlayer strongest = strongest(playerSet);
            if (team2.size() < (candidate.getPlayers().size()+1)/2) {
                if (team1Rating <= team2Rating) {
                    team1.add(strongest);
                    team1Rating += strongest.getRating();
                } else {
                    team2.add(strongest);
                    team2Rating += strongest.getRating();
                }
            } else {
                team1.add(strongest);
                team1Rating += strongest.getRating();
            }
        }
    }

    public LinkedList<QueueingPlayer> getTeam1() {
        return team1;
    }

    public LinkedList<QueueingPlayer> getTeam2() {
        return team2;
    }

    public int getTeam1Rating() {
        return team1Rating;
    }

    public int getTeam2Rating() {
        return team2Rating;
    }

    private QueueingPlayer strongest(HashSet<QueueingPlayer> players) {
        int maxRating = -1;
        QueueingPlayer result = null;
        for (QueueingPlayer player : players)
            if (player.getRating() >  maxRating) {
                maxRating = player.getRating();
                result = player;
            }
        return result;
    }
}

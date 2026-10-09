package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;

import java.util.HashSet;
import java.util.LinkedList;

public class PreliminaryTeams {
    private LinkedList<QueueingPlayer> team1 = new LinkedList<>(), team2 = new LinkedList<>();
    private int team1Rating = 0, team2Rating = 0;

    public PreliminaryTeams(MatchCandidate candidate, BrickballFormat format) {

        HashSet<QueueingPlayer> playerSet = (HashSet<QueueingPlayer>) candidate.getPlayers().clone();
        while (!playerSet.isEmpty()) {
            QueueingPlayer strongest = strongest(playerSet);
            playerSet.remove(strongest);
            if (team2.size() < (candidate.getPlayers().size()+1)/2) {
                if (team1Rating <= team2Rating) {
                    team1.add(strongest);
                    team1Rating += strongest.rating();
                } else {
                    team2.add(strongest);
                    team2Rating += strongest.rating();
                }
            } else {
                team1.add(strongest);
                team1Rating += strongest.rating();
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
            if (player.rating() >  maxRating) {
                maxRating = player.rating();
                result = player;
            }
        return result;
    }
}

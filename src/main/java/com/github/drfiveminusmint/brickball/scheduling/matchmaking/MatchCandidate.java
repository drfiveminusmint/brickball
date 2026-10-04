package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import java.util.HashSet;

public class MatchCandidate implements Cloneable {
    private int highRating = -1, lowRating = Integer.MAX_VALUE;
    private HashSet<QueueingPlayer> players = new HashSet<>();

    public void addPlayer(QueueingPlayer player) {
        if (player.getRating() > highRating)
            highRating = player.getRating();
        if (player.getRating() < lowRating)
            lowRating = player.getRating();
        players.add(player);
    }

    public int getHighRating() {
        return highRating;
    }

    public int getLowRating() {
        return lowRating;
    }

    public HashSet<QueueingPlayer> getPlayers() {
        return players;
    }

    public MatchCandidate clone() {
        MatchCandidate result = new MatchCandidate();
        result.lowRating = lowRating; result.highRating = highRating;
        result.players = (HashSet<QueueingPlayer>) players.clone();
        return result;
    }
}

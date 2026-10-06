package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QueueingPlayer implements Comparable<QueueingPlayer> {
    private final Player player;
    private final long joinTime;
    private final int rating;
    public QueueingPlayer(Player player, long joinTime, int rating) {
        this.player = player;
        this.joinTime = joinTime;
        this.rating = rating;
    }

    public Player getPlayer() {
        return player;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public int getRating() {
        return rating;
    }

    @Override
    public int compareTo(@NotNull QueueingPlayer queueingPlayer) {
        // if this fails we have bigger problems
        return (int) (queueingPlayer.joinTime - joinTime);
    }
}

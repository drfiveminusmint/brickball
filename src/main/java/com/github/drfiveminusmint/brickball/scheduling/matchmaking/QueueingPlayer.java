package com.github.drfiveminusmint.brickball.scheduling.matchmaking;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record QueueingPlayer(Player player, long joinTime, int rating) implements Comparable<QueueingPlayer> {

    @Override
    public int compareTo(@NotNull QueueingPlayer queueingPlayer) {
        // if this fails we have bigger problems
        return (int) (queueingPlayer.joinTime - joinTime);
    }
}

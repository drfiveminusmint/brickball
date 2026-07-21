package com.github.drfiveminusmint.brickball.events.event;

import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchStartEvent extends Event implements Cancellable {
    private static final HandlerList list = new HandlerList();
    private final ArenaTemplate arenaTemplate;
    private final BrickballFormat format;
    private final BrickballMatch match;
    private boolean cancelled = false;

    public MatchStartEvent(BrickballMatch match, BrickballFormat format, ArenaTemplate arenaTemplate) {
        this.match = match;
        this.format = format;
        this.arenaTemplate = arenaTemplate;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return list;
    }

    // would have appreciated if this requirement was documented ANYWHERE
    public static @NotNull HandlerList getHandlerList() {
        return list;
    }

    public BrickballFormat getFormat() {
        return format;
    }

    public BrickballMatch getMatch() {
        return match;
    }

    public ArenaTemplate getArenaTemplate() {return arenaTemplate;}

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}

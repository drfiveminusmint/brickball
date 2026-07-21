package com.github.drfiveminusmint.brickball.events.event;

import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchEndEvent extends Event {
    private static final HandlerList list = new HandlerList();
    private final MatchResult result;
    private final BrickballFormat format;
    private final BrickballMatch match;

    public MatchEndEvent(BrickballMatch match, BrickballFormat format, MatchResult result) {
        this.format = format;
        this.result = result;
        this.match = match;
    }

    public MatchResult getResult() { return result; }

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
}

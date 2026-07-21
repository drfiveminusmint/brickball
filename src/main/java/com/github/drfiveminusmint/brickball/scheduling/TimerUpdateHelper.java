package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchState;
import org.bukkit.scheduler.BukkitRunnable;

public class TimerUpdateHelper extends BukkitRunnable {
    final BrickballMatch match;

    public TimerUpdateHelper(BrickballMatch brickballMatch) {
        match = brickballMatch;
    }
    @Override
    public void run() {
        if (match.getState() == MatchState.RUNNING)
            match.tickTimer();
        // Fallback in case of a resource leak
        else if (match.getState() == MatchState.FROZEN)
            cancel();
    }
}

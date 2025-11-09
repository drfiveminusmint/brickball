package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchState;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class TimerUpdateHelper extends BukkitRunnable {
    final BrickballMatch match;

    public TimerUpdateHelper(BrickballMatch brickballMatch) {
        match = brickballMatch;
    }
    @Override
    public void run() {
        if (match.getState() == MatchState.RUNNING)
            match.tickTimer();
    }
}

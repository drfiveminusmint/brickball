package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import org.jetbrains.annotations.NotNull;

// Used to ensure matches only start after the arena has been built
public class MatchStartTask implements SyncTask{

    private final BrickballMatch match;
    private final int priority;

    public MatchStartTask(BrickballMatch match, int priority) {
        this.match = match;
        this.priority = priority;
    }
    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof SyncTask task)
            return priority - task.getPriority();
        return 0;
    }

    @Override
    public void run() {
        match.startMatch();
    }
}

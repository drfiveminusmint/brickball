package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.stats.Leaderboard;
import org.jetbrains.annotations.NotNull;

public class LeaderboardUpdateTask implements PriorityTask {
    private final int priority = 0;
    private final Leaderboard leaderboard;

    public LeaderboardUpdateTask(Leaderboard board) {
        leaderboard = board;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public int compareTo(@NotNull PriorityTask task) {
        return priority - task.getPriority();
    }

    @Override
    public void run() {
        leaderboard.update();
    }
}

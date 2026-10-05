package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.stats.FormatStats;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LoadStatsTask implements IOTask {
    private final int priority;
    private final File file;
    private final FormatStats formatStats;
    private final BrickballFormat format;

    public LoadStatsTask(int priority, File origin, FormatStats destination, BrickballFormat format) {
        this.priority = priority;
        this.file = origin;
        this.formatStats = destination;
        this.format = format;
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
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }

    @Override
    public void run() {
        formatStats.readFromFile(file);
        if (Brickball.getInstance().getLeaderboard(format) != null)
            Brickball.getInstance().getLeaderboard(format).update();
    }
}

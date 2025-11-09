// Manages Brickball tasks
package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;

import java.util.PriorityQueue;

public class BrickballScheduler {
    private BrickballTaskManager syncTasks, fileTasks, miscTasks;

    public BrickballScheduler() {
        syncTasks = new BrickballTaskManager(1);
        fileTasks = new BrickballTaskManager(4);
        miscTasks = new BrickballTaskManager(16);
        syncTasks.runTaskTimer(Brickball.getPlugin(Brickball.class), 0,1);
        fileTasks.runTaskTimerAsynchronously(Brickball.getPlugin(Brickball.class), 0,1);
        miscTasks.runTaskTimerAsynchronously(Brickball.getPlugin(Brickball.class), 0,1);
    }

    // Sort submitted tasks
    public void submitTask(PriorityTask task)
    {
        switch (task) {
            case SyncTask syncTask -> syncTasks.submitTask(task);
            case IOTask ioTask -> fileTasks.submitTask(task);
            case null, default -> miscTasks.submitTask(task);
        }
    }
}

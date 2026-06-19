// Manages Brickball tasks
package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;

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

    public void shutdown() {
        // shut down our task managers
        syncTasks.cancel();
        fileTasks.cancel();
        miscTasks.cancel();

        // force all tasks to complete synchronously
        // tasks may submit other tasks, so we have to run this multiple times
        while (syncTasks.hasTask() || fileTasks.hasTask() || miscTasks.hasTask()) {
            miscTasks.forceCompleteAll();
            fileTasks.forceCompleteAll();
            syncTasks.forceCompleteAll();
        }
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

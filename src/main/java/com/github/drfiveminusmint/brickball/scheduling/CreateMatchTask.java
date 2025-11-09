package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import org.jetbrains.annotations.NotNull;

public class CreateMatchTask implements SyncTask {
    private int priority = 0;
    private String id;

    public CreateMatchTask(String templateID, int prio)
    {
        id = templateID;
        priority = prio;
    }

    @Override
    public void run() {
        Brickball.getInstance().getMatchManager().startMatch(Brickball.getInstance().getTemplateManager().findTemplate(id), priority).freeze();
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
}

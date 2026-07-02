package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

// this should probably be removed...
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
        BrickballMatch match = Brickball.getInstance().getMatchManager().createMatch(Brickball.getInstance().getTemplateManager().findTemplate(id), priority);
        if (match == null) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "CreateMatchTask failed for template = " + id);
            return;
        }
        Brickball.getInstance().getMatchManager().freezeMatch(match);
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

package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

// Handle the synchronized parts of match creation
public class CreateMatchTask implements SyncTask {
    private final int priority;
    private final String id;

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
    public int compareTo(@NotNull PriorityTask task) {
        return priority - task.getPriority();
    }

    @Override
    public int getCount() {
        return 0;
    }
}

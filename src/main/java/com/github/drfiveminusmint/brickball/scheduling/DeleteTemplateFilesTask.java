package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class DeleteTemplateFilesTask implements IOTask {
    private final int priority = -99;
    private final ArenaTemplate arenaTemplate;

    public DeleteTemplateFilesTask(ArenaTemplate template) {
        arenaTemplate = template;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }

    @Override
    public void run() {
        // remove the template's directory
        File target = new File(Brickball.getTemplatesFolder(), arenaTemplate.getID() + ".bbmap");
        if (target.exists())
            target.delete();
        else
            return;
        target = new File(Brickball.getTemplatesFolder(), arenaTemplate.getID());
        for (File child : target.listFiles())
            child.delete();
        target.delete();
    }

    @Override
    public int getCount() {
        return 0;
    }
}

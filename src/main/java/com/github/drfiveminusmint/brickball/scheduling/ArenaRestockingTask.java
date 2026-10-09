package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchManager;
import com.github.drfiveminusmint.brickball.match.MatchState;
import com.github.drfiveminusmint.brickball.util.Counter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaRestockingTask implements PriorityTask {

    int priority = -5;
    private BrickballMatch[] matchList;
    private ConcurrentHashMap<String, Counter> templateCounts = new ConcurrentHashMap<>();

    public ArenaRestockingTask(BrickballMatch[] matches, ArrayList<ArenaTemplate> templateIDSet)
    {
        matchList = matches;
        // Initialize counter
        for (ArenaTemplate template : templateIDSet)
            templateCounts.put(template.getID(), new Counter());
    }

    @Override
    public void run() {
        for (BrickballMatch match : matchList) {
            if (match == null) continue;
            if (match.getState() != MatchState.FROZEN) continue;
            templateCounts.get(match.getMapID()).increment();
        }
        for (String s : templateCounts.keySet()) {
            if (templateCounts.get(s).value() == 0)
            {
                BrickballMatch match = Brickball.getInstance().getMatchManager().createMatch(Brickball.getInstance().getTemplateManager().findTemplate(s), -5);
                if (match != null)
                    Brickball.getInstance().getMatchManager().freezeMatch(match);
            }
        }
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


package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchState;
import com.github.drfiveminusmint.brickball.util.Counter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaRestockingTask implements PriorityTask {

    int priority = -5;
    private BrickballMatch[] matchList;
    private ConcurrentHashMap<String, Counter> templateCounts = new ConcurrentHashMap<>();

    public ArenaRestockingTask(BrickballMatch[] matches, Set<String> templateIDSet)
    {
        matchList = matches;
        // Initialize counter
        for (String template : templateIDSet)
            templateCounts.put(template, new Counter());
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
                Brickball.getInstance().getScheduler().submitTask(new CreateMatchTask(s, -5));
            }
        }
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


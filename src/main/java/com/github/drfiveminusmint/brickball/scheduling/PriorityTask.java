package com.github.drfiveminusmint.brickball.scheduling;

public interface PriorityTask extends Runnable, Comparable<PriorityTask> {
    int getPriority();
    int getCount();
}

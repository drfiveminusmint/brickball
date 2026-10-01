package com.github.drfiveminusmint.brickball.scheduling;

public interface PriorityTask extends Runnable, Comparable {
    int getPriority();
    int getCount();
}

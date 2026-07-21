package com.github.drfiveminusmint.brickball.scheduling;

public interface PriorityTask extends Runnable, Comparable {
    public int getPriority();
    public int getCount();
}

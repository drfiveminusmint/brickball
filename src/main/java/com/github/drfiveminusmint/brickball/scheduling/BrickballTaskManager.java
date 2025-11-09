package com.github.drfiveminusmint.brickball.scheduling;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BrickballTaskManager extends BukkitRunnable {
    private int tasksPerTick = 1;
    private ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public BrickballTaskManager (int taskRate) {
        tasksPerTick = taskRate;
    }
    @Override
    public void run() {
        int stepsDone = 0;
        while (!tasks.isEmpty() && stepsDone++ < tasksPerTick)
            tasks.poll().run();
    }
    public void setTaskRate(int taskRate) { tasksPerTick = taskRate; }
    public void submitTask(Runnable r) {tasks.add(r);}
}

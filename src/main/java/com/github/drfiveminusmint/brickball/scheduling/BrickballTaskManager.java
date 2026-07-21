package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class BrickballTaskManager extends BukkitRunnable {
    private int tasksPerTick = 1;
    private boolean shutdown = false;
    private final PriorityBlockingQueue<PriorityTask> tasks = new PriorityBlockingQueue<>();


    public BrickballTaskManager (int taskRate) {
        tasksPerTick = taskRate;
    }
    @Override
    public void run() {
        int stepsDone = 0;
        while (!tasks.isEmpty() && stepsDone++ < tasksPerTick) {
            PriorityTask task = tasks.poll();
            task.run();
            // send tasks that still have work to do to the back
            if (task.getCount() > 0) tasks.add(task);
        }
        if (shutdown) cancel();
    }
    public void setTaskRate(int taskRate) { tasksPerTick = taskRate; }
    public void submitTask(PriorityTask task) {tasks.add(task);}

    public boolean hasTask() {return !tasks.isEmpty();}

    // Complete all tasks on the next tick
    public void completeAndShutdown() {
        tasksPerTick = Integer.MAX_VALUE;
        shutdown = true;
    }
}

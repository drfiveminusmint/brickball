package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import org.bukkit.Bukkit;
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

    public boolean hasTask() {return !tasks.isEmpty();}

    // Forcibly complete every thread in the queue
    public void forceCompleteAll() {
        // only allow this from the main thread
        if (!Bukkit.getServer().getScheduler().getMainThreadExecutor(Brickball.getInstance()).equals(Thread.currentThread()))
            throw new IllegalThreadStateException("ForceCompleteAll must be called from the main thread.");
        if (!isCancelled()) cancel();
        while (!tasks.isEmpty()) tasks.poll().run();
    }
}

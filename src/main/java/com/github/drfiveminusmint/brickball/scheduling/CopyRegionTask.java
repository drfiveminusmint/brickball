package com.github.drfiveminusmint.brickball.scheduling;


import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;

public class CopyRegionTask implements SyncTask {
    int priority = 0;
    Region area;
    BlockVector3 sourceLoc;
    World sourceWorld;
    File dir;
    String fileName;

    public CopyRegionTask(Region region, BlockVector3 origin, World world, File directory, String name, int prio) {
        area = region;
        dir = directory;
        fileName = name;
        priority = prio;
        sourceLoc = origin;
        sourceWorld = world;
    }

    @Override
    public void run() {
        Brickball.getInstance().getLogger().log(Level.INFO, "Saving Region:" + area);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(area);
        ForwardExtentCopy copy = new ForwardExtentCopy(
                sourceWorld, area, sourceLoc, clipboard, sourceLoc);
        try {
            Operations.complete(copy);
        } catch (WorldEditException ex) {
            ex.printStackTrace();
        }
        Brickball.getInstance().getScheduler().submitTask(new SaveSchematicTask(clipboard, dir, fileName, priority));
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public int getPriority() { return priority; }

    @Override
    public int getCount() {
        return 0;
    }
}

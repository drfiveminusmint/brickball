package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class RegionCleanupTask implements SyncTask {
    private int priority = 0;
    private World world;

    private Region area;

    public RegionCleanupTask(CuboidRegion region, World gameWorld, int prio) {
        priority = prio;
        world = gameWorld;
        area = region;
    }
    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public void run() {
        try (EditSession cleanupSession = WorldEdit.getInstance().newEditSession(world);){
            cleanupSession.setBlocks(area,BlockTypes.AIR.getDefaultState().toBaseBlock());
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "RUH ROH RHAGGY");
        }
    }
    @Override
    public int getPriority() { return priority; }
}

package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchState;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.Clipboard;
import java.util.List;

public class SchematicsPasteTask implements SyncTask {
    private final int priority;
    private int count;
    private final List<ClipboardHolder> schematics;
    private final List<BlockVector3> offsets;
    private final World world;
    private final @Nullable BrickballMatch notify;

    public SchematicsPasteTask(List<ClipboardHolder> schematics, List<BlockVector3> offsets, World world, int priority, @Nullable BrickballMatch notify) {
        this.priority = priority;
        this.schematics = schematics;
        this.world = world;
        this.offsets = offsets;
        this.notify = notify;
        this.count = schematics.size();
    }
    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public void run() {
        int index = schematics.size() - count--;
        ClipboardHolder clipboardHolder = schematics.get(index);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)){
            Operation operation = clipboardHolder
                    .createPaste(editSession)
                    .to(offsets.get(index))
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
        // if this is the last schematic to paste, start the match
        if (notify != null)
            notify.reportLoadingProgress(index+1, schematics.size());
    }
    @Override
    public int getPriority() { return priority; }

    @Override
    public int getCount() {
        return count;
    }
}

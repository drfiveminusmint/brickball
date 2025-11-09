package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class SchematicPasteTask implements SyncTask {
    private int priority = 0;
    private ClipboardHolder clipboard;
    private World world;
    private BlockVector3 location;

    public SchematicPasteTask(ClipboardHolder schematic, World gameWorld, BlockVector3 origin, int prio) {
        priority = prio;
        clipboard = schematic;
        world = gameWorld;
        location = origin;
    }
    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public void run() {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)){
            Operation operation = clipboard
                    .createPaste(editSession)
                    .to(location)
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }
    @Override
    public int getPriority() { return priority; }
}

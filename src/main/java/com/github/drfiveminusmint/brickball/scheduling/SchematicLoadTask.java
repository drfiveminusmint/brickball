package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;

public class SchematicLoadTask implements SyncTask {
    private int priority = 0;
    private File path;
    private World world;
    private BlockVector3 location;

    public SchematicLoadTask(File file, World gameWorld, BlockVector3 origin, int prio) {
        path = file;
        priority = prio;
        location = origin;
        world = gameWorld;
    }
    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public void run() {
        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(path);
        if (format == null)
        {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error finding file format for " + path.getAbsolutePath());
            return;
        }
        try  {
            ClipboardReader reader = format.getReader(new FileInputStream(path));
            clipboard = reader.read();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error loading file " + path.getAbsolutePath());
            return;
        }
        String[] coords = path.getName().split("-");
        location = location.add(Integer.parseInt(coords[0])*16, 0, Integer.parseInt(coords[1])*16);
        // Tell the main thread worker to paste this schematic
        Brickball.getInstance().getScheduler().submitTask(new SchematicPasteTask(new ClipboardHolder(clipboard), world, location, priority));
    }
    @Override
    public int getPriority() { return priority; }
}

package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ArenaLoadTask implements SyncTask {
    private int priority, count;
    private List<File> files;
    private final List<ClipboardHolder> loadedSchematics;
    private final List<BlockVector3> offsets;
    private World world;
    private BlockVector3 origin;
    private final @Nullable BrickballMatch notifyMatch;

    public ArenaLoadTask(List<File> files, World world, BlockVector3 origin, int priority, @Nullable BrickballMatch notifyMatch) {
        this.files = files;
        this.priority = priority;
        this.origin = origin;
        this.world = world;
        this.notifyMatch = notifyMatch;
        this.count = files.size();
        this.loadedSchematics = new ArrayList<>(files.size());
        this.offsets = new ArrayList<>(files.size());
    }
    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public void run() {
        File path = files.get(files.size()-count--);
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
        offsets.add(origin.add(Integer.parseInt(coords[0])*16, 0, Integer.parseInt(coords[1])*16));
        loadedSchematics.add(new ClipboardHolder(clipboard));
        // If we're done, tell the main thread worker to paste these schematics
        if (count == 0)
            Brickball.getInstance().getScheduler().submitTask(new SchematicsPasteTask(loadedSchematics, offsets, world, priority, notifyMatch));
    }
    @Override
    public int getPriority() { return priority; }

    @Override
    public int getCount() {
        return count;
    }
}

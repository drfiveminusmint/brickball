package com.github.drfiveminusmint.brickball.scheduling;

import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class SaveSchematicTask implements IOTask {
    int priority;
    private Clipboard schem;
    private File dir;
    private String fileName;

    public SaveSchematicTask(Clipboard clipboard, File directory, String name, int prio) {
        priority = prio;
        dir = directory;
        fileName = name;
        schem = clipboard;
    }

    @Override
    public void run() {
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, fileName + ".schem");
        if (file.exists()) file.delete();
        try {
            ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(new FileOutputStream(file));
            writer.write(schem);
            writer.close();
        } catch (IOException ex) {
            // If my programming professors could see me now, I don't know if they'd laugh or cry.
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error saving schematic!");
            ex.printStackTrace();
        }
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof PriorityTask task)
            return priority - task.getPriority();
        return 0;
    }
    @Override
    public int getPriority() { return priority; }
}

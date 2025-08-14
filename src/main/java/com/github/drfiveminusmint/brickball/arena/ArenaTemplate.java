package com.github.drfiveminusmint.brickball.arena;

import com.github.drfiveminusmint.brickball.Brickball;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;

public class ArenaTemplate {
    private final ProtectedRegion masterRegion, spawnA, doorA, spawnB, doorB;
    private final Location brickSpawn;
    private final String ID;


    public ArenaTemplate (String id, ProtectedRegion masterRegion, ProtectedRegion doorA, ProtectedRegion spawnA, ProtectedRegion doorB, ProtectedRegion spawnB, Location brickSpawn) {
        this.ID = id;
        this.masterRegion = masterRegion;
        this.doorA = doorA;
        this.spawnA = spawnA;
        this.doorB = doorB;
        this.spawnB = spawnB;
        this.brickSpawn = brickSpawn;
    }

    public static ArenaTemplate createByID(String id, Location spawnLoc, World world) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(world);
        ArenaTemplate result = new ArenaTemplate(id,
                manager.getRegion(id+"Master"),
                manager.getRegion(id+"DoorA"),
                manager.getRegion(id+"SpawnA"),
                manager.getRegion(id+"DoorB"),
                manager.getRegion(id+"SpawnB"),
                spawnLoc);
        result.saveSchematic(world);
        return result;
    }

    public void saveSchematic (World sourceWorld) {
        CuboidRegion schemRegion = new CuboidRegion(masterRegion.getMinimumPoint(), masterRegion.getMaximumPoint());
        BlockArrayClipboard clipboard = new BlockArrayClipboard(schemRegion);
        ForwardExtentCopy copy = new ForwardExtentCopy(
                sourceWorld, schemRegion, clipboard, masterRegion.getMinimumPoint());
        try {
            Operations.complete(copy);
            File schemFile = new File(Brickball.getTemplatesFolder(), ID + ".schem");
            if (schemFile.exists()) schemFile.delete();
            ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(new FileOutputStream(schemFile));
            writer.write(clipboard);
            writer.close();
        }
        catch (Exception e) {
            // If my programming professors could see me now, I don't know if they'd laugh or cry.
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error saving schematic!");
            e.printStackTrace();
        }
    }

    public String getID() {
        return ID;
    }
    public ProtectedRegion getMasterRegion() {
        return masterRegion;
    }
    public ProtectedRegion getSpawnA() {
        return spawnA;
    }
    public ProtectedRegion getDoorA() {
        return doorA;
    }
    public ProtectedRegion getSpawnB() {
        return spawnB;
    }
    public ProtectedRegion getDoorB() {
        return doorB;
    }
    public Location getBrickSpawn() {
        return brickSpawn;
    }
}

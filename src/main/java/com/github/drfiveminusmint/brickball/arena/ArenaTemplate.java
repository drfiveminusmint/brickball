package com.github.drfiveminusmint.brickball.arena;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.scheduling.CopyRegionTask;
import com.github.drfiveminusmint.brickball.util.WGUtils;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import org.bukkit.Location;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class ArenaTemplate {
    private final ProtectedRegion masterRegion, spawnA, doorA, spawnB, doorB;
    private final HashSet<ProtectedRegion> deathRegions;
    private final Location brickSpawn;
    private final String ID;


    public ArenaTemplate (String id, ProtectedRegion masterRegion, ProtectedRegion doorA, ProtectedRegion spawnA, ProtectedRegion doorB, ProtectedRegion spawnB, HashSet<ProtectedRegion> deathRegions, Location brickSpawn) {
        this.ID = id;
        this.masterRegion = masterRegion;
        this.doorA = doorA;
        this.spawnA = spawnA;
        this.doorB = doorB;
        this.spawnB = spawnB;
        this.brickSpawn = brickSpawn;
        this.deathRegions = deathRegions;
    }

    public static ArenaTemplate createByID(String id, Location spawnLoc, World world, boolean addSchem) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(world);
        ProtectedRegion masterRegion = manager.getRegion(id+"Master");
        HashSet<ProtectedRegion> dregions = new HashSet<>();
        for (ProtectedRegion region : manager.getApplicableRegions(masterRegion))
        {
            if (masterRegion.equals(region.getParent()) && region.getId().contains("death"))
                dregions.add(region);
        }
        ArenaTemplate result = new ArenaTemplate(id,
                masterRegion,
                manager.getRegion(id+"DoorA"),
                manager.getRegion(id+"SpawnA"),
                manager.getRegion(id+"DoorB"),
                manager.getRegion(id+"SpawnB"),
                dregions,
                spawnLoc);
        if (addSchem)
            result.saveSchematic(world);
        return result;
    }

    public void saveSchematic (World sourceWorld) {
        File schemDirectory = new File(Brickball.getTemplatesFolder(), ID);
        for (Region region : WGUtils.subdivideCuboidRegion(WorldEditRegionConverter.convertToRegion(masterRegion))) {
            BlockVector3 distance = region.getMinimumPoint().subtract(masterRegion.getMinimumPoint());
            Brickball.getInstance().getScheduler().submitTask(new CopyRegionTask(
                    region,
                    masterRegion.getMinimumPoint(),
                    sourceWorld,
                    schemDirectory,
                    String.format("%d-%d-",distance.x()/16,distance.z()/16),
                    0));
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

    public HashSet<ProtectedRegion> getDeathRegions() {
        return deathRegions;
    }
}

package com.github.drfiveminusmint.brickball.arena;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.scheduling.*;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.github.drfiveminusmint.brickball.util.WGUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class BrickballArena {
    private final ProtectedRegion masterRegion, spawnA, doorA,spawnB, doorB;
    private final World gameWorld;
    private final Location[] spawnLocations = new Location[2];
    private final BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE};
    private final Location brickSpawn;
    private final String templateID;

    public BrickballArena(ArenaTemplate template, Location minPoint, int matchID) {
        gameWorld = new BukkitWorld(minPoint.getWorld());
        // Calculate offset
        BlockVector3 offset = new BlockVector3(minPoint.blockX(), minPoint.blockY(), minPoint.blockZ()).subtract(template.getMasterRegion().getMinimumPoint());
        masterRegion = WGUtils.cloneRegionBetweenWorlds(template.getMasterRegion(), String.format("master%d", matchID), gameWorld, offset);
        spawnA = WGUtils.cloneRegionBetweenWorlds(template.getSpawnA(), String.format("spawna%d", matchID), gameWorld, offset);
        spawnLocations[0] = WGUtils.blockVectorToLocation(WGUtils.getCenterFloor(spawnA), gameWorld);
        doorA = WGUtils.cloneRegionBetweenWorlds(template.getDoorA(), String.format("doora%d", matchID), gameWorld, offset);
        spawnB = WGUtils.cloneRegionBetweenWorlds(template.getSpawnB(), String.format("spawnb%d", matchID), gameWorld, offset);
        spawnLocations[1] = WGUtils.blockVectorToLocation(WGUtils.getCenterFloor(spawnB), gameWorld);
        doorB = WGUtils.cloneRegionBetweenWorlds(template.getDoorB(), String.format("doorb%d", matchID), gameWorld, offset);
        brickSpawn = new Location(minPoint.getWorld(), template.getBrickSpawn().x()+(offset.x()*1.0d), template.getBrickSpawn().y()+0.1+(offset.y()*1.0d), template.getBrickSpawn().z()+(offset.z()*1.0d));
        templateID = template.getID();
    }

    public void generateArena(int priority) {
        File directory = new File(Brickball.getTemplatesFolder(), templateID);
        for (File f : Objects.requireNonNull(directory.listFiles(pathname -> {
            try {
                if (pathname.getName().contains(".schem"))
                    return true;
            } catch (Exception ex) {
                return false;
            }
            return false;
        }))) {
            Brickball.getInstance().getScheduler().submitTask(new SchematicLoadTask(
                    f,
                    gameWorld,
                    masterRegion.getMinimumPoint(),
                    priority));
        }
    }


    public void cleanupArena() {
        Brickball.getInstance().getLogger().log(Level.SEVERE, "Cleaning Up");
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(gameWorld);
        manager.removeRegion(masterRegion.getId());
        manager.removeRegion(spawnA.getId());
        manager.removeRegion(doorA.getId());
        manager.removeRegion(spawnB.getId());
        manager.removeRegion(doorB.getId());
        for (CuboidRegion chunk : WGUtils.subdivideCuboidRegion(WorldEditRegionConverter.convertToRegion(masterRegion))) {
            Brickball.getInstance().getScheduler().submitTask(new RegionCleanupTask(chunk, gameWorld, 0));
        }
    }

    public void openDoors() {
        try (EditSession openSession = WorldEdit.getInstance().newEditSession(gameWorld)){
            openSession.setBlocks(WorldEditRegionConverter.convertToRegion(doorA),
                    BlockTypes.AIR.getDefaultState().toBaseBlock());
            openSession.setBlocks(WorldEditRegionConverter.convertToRegion(doorB),
                    BlockTypes.AIR.getDefaultState().toBaseBlock());
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "RUH ROH RHAGGY");
        }
    }

    public void closeDoors() {
        try (EditSession openSession = WorldEdit.getInstance().newEditSession(gameWorld)){
            openSession.setBlocks(WorldEditRegionConverter.convertToRegion(doorA),
                    teamColors[0].paneType);
            openSession.setBlocks(WorldEditRegionConverter.convertToRegion(doorB),
                    teamColors[1].paneType);
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "RUH ROH RHAGGY");
        }
        // re-dye doors to fix connection issues
        // disgusting hack, replace this ASAP
        setTeamColor(teamColors[0], 0);
        setTeamColor(teamColors[1], 1);
    }

    public boolean checkLocationInSpawn(Location location, int team) {
        return switch (team) {
            case 0 -> spawnA.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
            case 1 -> spawnB.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
            default -> false;
        };
    }

    public boolean checkLocationInbounds(Location location) {
        return masterRegion.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
    }

    public Location getSpawnLocation (int team) {return spawnLocations[team];}
    public Location getBrickSpawn () {return brickSpawn;}

    public void setTeamColor(BrickballColor teamColor, int i) {

        teamColors[i] = teamColor;
        //Dye doors
        BlockVector3 lowCorner;
        BlockVector3 highCorner;
        if (i == 0) {
            lowCorner = doorA.getMinimumPoint().add(-1,-1,-1);
            highCorner = doorA.getMaximumPoint().add(1,1,1);
        } else {
            lowCorner = doorB.getMinimumPoint().add(-1,-1,-1);
            highCorner = doorB.getMaximumPoint().add(1,1,1);
        }
        try (EditSession dyeSession = WorldEdit.getInstance().newEditSession(gameWorld)){
            dyeSession.replaceBlocks(new CuboidRegion(lowCorner,highCorner), WGUtils.GLASS_ALL, teamColor.glassType);
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "RUH ROH RHAGGY");
        }
    }
    public String getTemplateID() {return templateID;}
}

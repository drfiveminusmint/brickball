package com.github.drfiveminusmint.brickball.arena;

import com.github.drfiveminusmint.brickball.Brickball;
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
import java.util.logging.Level;

public class BrickballArena {
    private ProtectedRegion masterRegion, spawnA, doorA,spawnB, doorB;
    private World gameWorld;
    private Location[] spawnLocations = new Location[2];
    private BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE};
    private Location brickSpawn;
    private String templateID;

    public BrickballArena(ArenaTemplate template, World destinationWorld) {
        gameWorld = destinationWorld;
        masterRegion = WGUtils.cloneRegionBetweenWorlds(template.getMasterRegion(), gameWorld);
        spawnA = WGUtils.cloneRegionBetweenWorlds(template.getSpawnA(), gameWorld);
        spawnLocations[0] = WGUtils.blockVectorToLocation(WGUtils.getCenterFloor(spawnA), gameWorld);
        doorA = WGUtils.cloneRegionBetweenWorlds(template.getDoorA(),gameWorld);
        spawnB = WGUtils.cloneRegionBetweenWorlds(template.getSpawnB(), gameWorld);
        spawnLocations[1] = WGUtils.blockVectorToLocation(WGUtils.getCenterFloor(spawnB), gameWorld);
        doorB = WGUtils.cloneRegionBetweenWorlds(template.getDoorB(), gameWorld);
        brickSpawn = new Location(((BukkitWorld)destinationWorld).getWorld(), template.getBrickSpawn().x(), template.getBrickSpawn().y()+0.1, template.getBrickSpawn().z());
        templateID = template.getID();
    }

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

    public void generateArena() {
        File schematicFile = new File(Brickball.getTemplatesFolder().getAbsolutePath()+"/"+templateID+".schem");
        if (!schematicFile.exists()) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Could not find file " + schematicFile.getAbsolutePath());
            this.cleanupArena();
            return;
        }
        Clipboard clipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null)
        {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error finding file format for " + schematicFile.getAbsolutePath());
            this.cleanupArena();
            return;
        }
        try  {
            ClipboardReader reader = format.getReader(new FileInputStream(schematicFile));
            clipboard = reader.read();
        } catch (Exception e) {
            e.printStackTrace();
            Brickball.getInstance().getLogger().log(Level.SEVERE, "Error loading file " + schematicFile.getAbsolutePath());
            return;
        }
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(gameWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(masterRegion.getMinimumPoint())
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            e.printStackTrace();
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
        try (EditSession cleanupSession = WorldEdit.getInstance().newEditSession(gameWorld)){
            cleanupSession.setBlocks(WorldEditRegionConverter.convertToRegion(masterRegion),
                    BlockTypes.AIR.getDefaultState().toBaseBlock());
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "RUH ROH RHAGGY");
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
                    teamColors[0].glassType);
            openSession.setBlocks(WorldEditRegionConverter.convertToRegion(doorB),
                    teamColors[1].glassType);
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "RUH ROH RHAGGY");
        }
    }

    public boolean checkLocationInSpawn(Location location, int team) {
        return switch (team) {
            case 0 -> spawnA.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
            case 1 -> spawnB.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
            default -> false;
        };
    }

    public Location getSpawnLocation (int team) {return spawnLocations[team];}
    public Location getBrickSpawn () {return brickSpawn;}

    public void setTeamColor(BrickballColor teamColor, int i) {
        teamColors[i] = teamColor;
    }
}

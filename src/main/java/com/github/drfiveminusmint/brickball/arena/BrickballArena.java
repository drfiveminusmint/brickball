package com.github.drfiveminusmint.brickball.arena;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.scheduling.*;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.github.drfiveminusmint.brickball.util.WGUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import org.bukkit.Location;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class BrickballArena {
    private final ProtectedRegion masterRegion;
    private final ProtectedRegion[] spawnRegions = new ProtectedRegion[2];
    private final ProtectedRegion[] doors = new ProtectedRegion[2];
    private final HashSet<ProtectedRegion> deathRegions;
    private final World gameWorld;
    private final Location[] spawnLocations = new Location[2];
    private final Location[] lookLocations = new Location[2];
    private final BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE};
    private final Location brickSpawn;
    private final String templateID;
    private static final char[] teamLetters = {'a', 'b'};

    public boolean built = false;

    public BrickballArena(ArenaTemplate template, Location minPoint, int matchID) {
        gameWorld = new BukkitWorld(minPoint.getWorld());
        // Calculate offset
        BlockVector3 offset = new BlockVector3(minPoint.blockX(), minPoint.blockY(), minPoint.blockZ()).subtract(template.getMasterRegion().getMinimumPoint());
        masterRegion = WGUtils.cloneRegionBetweenWorlds(template.getMasterRegion(), String.format("master%d", matchID), gameWorld, offset);
        for (int i = 0; i < 2; i++) {
            spawnRegions[i] = WGUtils.cloneRegionBetweenWorlds(template.getSpawn(i), String.format("spawn%c%d", teamLetters[i], matchID), gameWorld, offset);
            spawnLocations[i] = WGUtils.blockVectorToLocation(WGUtils.getCenterFloor(spawnRegions[i]), gameWorld);
            doors[i] = WGUtils.cloneRegionBetweenWorlds(template.getDoor(i), String.format("door%c%d", teamLetters[i], matchID), gameWorld, offset);
            lookLocations[i] = WGUtils.blockVectorToLocation(WGUtils.getCenterFloor(doors[i]).add(0,1,0), gameWorld);
        }
        deathRegions = new HashSet<>();
        for (ProtectedRegion region : template.getDeathRegions())
            deathRegions.add(WGUtils.cloneRegionBetweenWorlds(region, String.format("%s%d", region.getId(), matchID), gameWorld, offset));
        brickSpawn = new Location(minPoint.getWorld(), template.getBrickSpawn().x()+(offset.x()*1.0d), template.getBrickSpawn().y()+0.1+(offset.y()*1.0d), template.getBrickSpawn().z()+(offset.z()*1.0d));
        templateID = template.getID();
    }

    public void generateArena(int priority, BrickballMatch notifyMatch) {
        File directory = new File(Brickball.getTemplatesFolder(), templateID);
        // get all files
        List<File> files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(directory.listFiles(pathname -> {
            try {
                if (pathname.getName().contains(".schem"))
                    return true;
            } catch (Exception ex) {
                return false;
            }
            return false;
        }))));
        Brickball.getInstance().getScheduler().submitTask(new ArenaLoadTask(files,
                gameWorld,
                masterRegion.getMinimumPoint(),
                priority,
                notifyMatch));
    }

    // Deconstruct the physical map and remove all regions
    public void cleanupArena() {
        Brickball.getInstance().getLogger().log(Level.INFO, "Cleaning Up");
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(gameWorld);
        manager.removeRegion(masterRegion.getId());
        for (ProtectedRegion region : doors)
            manager.removeRegion(region.getId());
        for (ProtectedRegion region : spawnRegions)
            manager.removeRegion(region.getId());
        for (ProtectedRegion region : deathRegions)
            manager.removeRegion(region.getId());
        // Prevent overwhelming the server by removing the map chunk-by-chunk
        for (CuboidRegion chunk : WGUtils.subdivideCuboidRegion(WorldEditRegionConverter.convertToRegion(masterRegion))) {
            Brickball.getInstance().getScheduler().submitTask(new RegionCleanupTask(chunk, gameWorld, 0));
        }
    }

    // Utility function to open the doors at the start of the round
    public void openDoors() {
        try (EditSession openSession = WorldEdit.getInstance().newEditSession(gameWorld)){
            openSession.setSideEffectApplier(SideEffectSet.none());
            for (ProtectedRegion region : doors)
                openSession.setBlocks(WorldEditRegionConverter.convertToRegion(region),
                        BlockTypes.AIR.getDefaultState().toBaseBlock());
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "WorldEdit encountered a problem opening the doors for an arena (is it up to date?)");
        }
    }

    // Utility function to close the doors at the start of the round
    public void closeDoors() {
        try (EditSession openSession = WorldEdit.getInstance().newEditSession(gameWorld)){
            openSession.setSideEffectApplier(SideEffectSet.none()
                    .with(SideEffect.NEIGHBORS, SideEffect.State.ON));
            for (int i = 0; i < doors.length; i++) {
                openSession.setBlocks(WorldEditRegionConverter.convertToRegion(doors[i]),
                        teamColors[i].paneType);
            }
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "WorldEdit encountered a problem closing the doors for an arena (is it up to date?)", ex);
        }
        for (ProtectedRegion region : doors) {
            // update the left and right column of blocks to properly connect the doors
            SideEffectSet fx = SideEffectSet.defaults()
                    .with(SideEffect.NEIGHBORS, SideEffect.State.ON);
            BlockVector3 direction;
            if (region.getMinimumPoint().x() == region.getMaximumPoint().x())   // door is oriented north-south
                direction = new BlockVector3(0, 0, 1);
            else                                                                // door is oriented east-west
                direction = new BlockVector3(1, 0, 0);
            for (int y = region.getMinimumPoint().y(); y <= region.getMaximumPoint().y(); y++) {
                BlockVector3 low = region.getMinimumPoint().withY(y).subtract(direction);
                BlockVector3 high = region.getMaximumPoint().withY(y).add(direction);
                try {
                    gameWorld.applySideEffects(low, gameWorld.getBlock(low), fx);
                    gameWorld.applySideEffects(high, gameWorld.getBlock(high), fx);
                } catch (WorldEditException ex) {
                    Brickball.getInstance().getLogger().log(Level.SEVERE, "WorldEdit encountered a problem closing the doors for an arena (is it up to date?)", ex);
                }
            }
        }
    }

    // Check if a location is within the specified team's spawnpoint
    public boolean checkLocationInSpawn(Location location, int team) {
        return spawnRegions[team].contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
    }

    // Check if a location is within one of the specified kill-regions on this map
    public boolean checkLocationInDeathRegion(Location location) {
        for (ProtectedRegion region : deathRegions)
            if (region.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ())))
                return true;
        return false;
    }

    public boolean checkLocationInbounds(Location location) {
        return masterRegion.contains(new BlockVector3(location.blockX(), location.blockY(), location.blockZ()));
    }

    public void setTeamColor(BrickballColor teamColor, int i) {

        teamColors[i] = teamColor;
        //Dye doors
        BlockVector3 lowCorner = doors[i].getMinimumPoint().add(-1,-1,-1);
        BlockVector3 highCorner = doors[i].getMaximumPoint().add(1,1,1);
        try (EditSession dyeSession = WorldEdit.getInstance().newEditSession(gameWorld);){
            dyeSession.setSideEffectApplier(SideEffectSet.none().with(SideEffect.NEIGHBORS, SideEffect.State.ON));
            dyeSession.replaceBlocks(new CuboidRegion(lowCorner,highCorner), WGUtils.GLASS_ALL, teamColor.glassType);
        } catch (WorldEditException ex) {
            Brickball.getInstance().getLogger().log(Level.SEVERE, "WorldEdit encountered a problem setting the team colors for an arena (is it up to date?)");
        }
    }

    public Location getSpawnLocation (int team) { return spawnLocations[team]; }
    public Location getBrickSpawn () { return brickSpawn; }
    public Location getLookLocation(int team) { return lookLocations[team]; }
    public String getTemplateID() { return templateID; }
}

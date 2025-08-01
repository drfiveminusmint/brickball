package com.github.drfiveminusmint.brickball.util;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class WGUtils {
    public static ProtectedRegion cloneRegionBetweenWorlds (ProtectedRegion master, World destination) {
        RegionManager destinationManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(destination);
        ProtectedRegion result;
        result = (master.getType().equals(RegionType.CUBOID)) ?
                new ProtectedCuboidRegion(master.getId(), master.getMinimumPoint(), master.getMaximumPoint()):
                new ProtectedPolygonalRegion(master.getId(), master.getPoints(), master.getMinimumPoint().y(),master.getMinimumPoint().y());
        result.copyFrom(master);
        if (destinationManager.hasRegion(master.getId())) destinationManager.removeRegion(master.getId());
        destinationManager.addRegion(result);
        return result;
    }

    public static ProtectedRegion cloneRegionBetweenWorlds (ProtectedRegion master, String newID, World destination, BlockVector3 offset) {
        RegionManager destinationManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(destination);
        ProtectedRegion result;
        if (master instanceof ProtectedCuboidRegion) {
            result = new ProtectedCuboidRegion(newID, master.getMinimumPoint().add(offset), master.getMaximumPoint().add(offset));
        } else {
            List<BlockVector2> points = new ArrayList<>();
            for (BlockVector2 point : master.getPoints())
                points.add(point.add(offset.toBlockVector2()));
            result = new ProtectedPolygonalRegion(newID, points, master.getMinimumPoint().add(offset).y(), master.getMaximumPoint().add(offset).y());
        }
        result.copyFrom(master);
        if (destinationManager.hasRegion(newID)) destinationManager.removeRegion(master.getId());
        destinationManager.addRegion(result);
        return result;
    }



    public static BlockVector3 getCenterFloor(ProtectedRegion region) {
        BlockVector3 width = region.getMaximumPoint().subtract(region.getMinimumPoint());
        BlockVector3 center = region.getMinimumPoint().add(width.divide(2));
        center.clampY(region.getMinimumPoint().y(),region.getMinimumPoint().y());
        return center;
    }

    public static org.bukkit.Location blockVectorToLocation(BlockVector3 bv, World w) {
        return new org.bukkit.Location(((BukkitWorld) w).getWorld(), bv.x()+0.5, bv.y(),bv.z()+0.5);
    }
}

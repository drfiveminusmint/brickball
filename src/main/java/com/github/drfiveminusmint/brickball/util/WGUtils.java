package com.github.drfiveminusmint.brickball.util;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.match.MatchState;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class WGUtils {
    public static final Set<BaseBlock> GLASS_ALL = Set.of(BlockTypes.BLACK_STAINED_GLASS.getDefaultState().toBaseBlock(),BlockTypes.WHITE_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.GRAY_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.RED_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.BLUE_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.YELLOW_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.GREEN_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.ORANGE_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.PURPLE_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.CYAN_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.MAGENTA_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.LIME_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.LIGHT_BLUE_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.PINK_STAINED_GLASS.getDefaultState().toBaseBlock(), BlockTypes.BROWN_STAINED_GLASS.getDefaultState().toBaseBlock());

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

    public static List<CuboidRegion> subdivideCuboidRegion(Region source) {
        List<CuboidRegion> subRegions = new ArrayList<>();
        int maxX = source.getMaximumPoint().x();
        int maxZ = source.getMaximumPoint().z();
        int minX = source.getMinimumPoint().x();
        int minZ = source.getMinimumPoint().z();
        for (int i = 0; i < Math.ceil((maxX-minX)/(16.0d)); i++)
            for (int j = 0; j < Math.ceil((maxZ-minZ)/(16.0d)); j++)
                subRegions.add(new CuboidRegion(
                        new BlockVector3(minX + i*16, source.getMinimumPoint().y(), minZ+j*16),
                        new BlockVector3(Math.min(minX + i*16+16, maxX), source.getMaximumPoint().y(), Math.min( minZ+j*16+16, maxZ))));
        return subRegions;

    }
    public static BlockVector3 getCenterFloor(ProtectedRegion region) {
        BlockVector3 width = region.getMaximumPoint().subtract(region.getMinimumPoint());
        BlockVector3 center = region.getMinimumPoint().add(width.divide(2));
        center = center.clampY(region.getMinimumPoint().y(),region.getMinimumPoint().y());
        return center;
    }

    public static org.bukkit.Location blockVectorToLocation(BlockVector3 bv, World w) {
        return new org.bukkit.Location(((BukkitWorld) w).getWorld(), bv.x()+0.5, bv.y(),bv.z()+0.5);
    }
}

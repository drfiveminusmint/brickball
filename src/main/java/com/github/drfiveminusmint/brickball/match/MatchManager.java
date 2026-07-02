package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.scheduling.ArenaRestockingTask;
import com.github.drfiveminusmint.brickball.scheduling.RegionCleanupTask;
import com.github.drfiveminusmint.brickball.util.WGUtils;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MatchManager {
    int currentMatchID = -1;
    final int MAX_MATCHES, ARENA_SPACING, ARENA_GRID_SIDE_LENGTH;
    private BrickballMatch[] matches;
    //Technically redundant but very useful if the number of matches becomes very large
    private final ConcurrentHashMap<Player,BrickballMatch> activePlayersMap = new ConcurrentHashMap<>();

    public MatchManager() {
        MAX_MATCHES = Brickball.getInstance().getConfig().getInt("maxMatches", 16);
        ARENA_SPACING = Brickball.getInstance().getConfig().getInt("arenaMaxSize", 240)+16;
        matches = new BrickballMatch[MAX_MATCHES];
        ARENA_GRID_SIDE_LENGTH = (int) Math.ceil(Math.sqrt(MAX_MATCHES));
    }
    public void stopAllMatches()
    {
        // TODO make this async
        for (BrickballMatch match : matches) {
            if (match == null) continue;
            match.shutdown();
        }
        currentMatchID = -1;
        matches = new BrickballMatch[MAX_MATCHES];
        activePlayersMap.clear();
    }

    //TODO search criteria
    public BrickballMatch getMatch(@Nullable MatchState state, @Nullable String mapID) {
        for (BrickballMatch match : matches) {
            if (match == null) continue;
            if (state != null && match.getState() != state) continue;
            if (mapID != null && !match.getMapID().equalsIgnoreCase(mapID)) continue;
            return match;
        }
        return null;
    }

    @Nullable
    public BrickballMatch getMatchByPlayer(Player player) {
        return activePlayersMap.get(player);
    }

    public @Nullable synchronized BrickballMatch createMatch(ArenaTemplate template, int priority) {
        if (Brickball.getInstance().getMatchWorld() == null) return null;
        BrickballMatch newMatch = getMatch(MatchState.FROZEN, template.getID());
        if (newMatch != null) {
            newMatch.unfreeze();
        } else for (int i = currentMatchID + 1; i != currentMatchID; i = (i+1) % MAX_MATCHES) {
            if (matches[i] == null) {
                newMatch = new BrickballMatch(template, getArenaMinPoint(i), i);
                newMatch.initialize(priority);
                matches[i] = newMatch;
                currentMatchID = i;
                Brickball.getInstance().getLogger().log(Level.INFO, "Successfully created match on map " + template.getID());
                break;
            }
        }
        // restock preloaded arenas unless this was an automated start
        if (priority >= 0 && Brickball.getInstance().isBackgroundGenerationEnabled())
            Brickball.getInstance().getScheduler().submitTask(new ArenaRestockingTask(matches, Brickball.getInstance().getTemplateManager().templates.keySet()));
        return newMatch;
    }

    // Shutdown all matches using a certain template and force cleanup
    // Because arenas use lazy cleanup usually, this is needed to make live changes to templates
    public synchronized void flushMatches(String templateID) {
        for (BrickballMatch match : matches) {
            if (match == null) continue;
            if (!match.getMapID().equalsIgnoreCase(templateID)) continue;
            endMatch(match, true);
        }
    }

    public void endMatch(BrickballMatch match) {
        endMatch(match, false);
    }

    // if forceCleanup is true, the map will always be cleaned up when the match is ended
    // otherwise standard freezing logic applies
    public void endMatch(BrickballMatch match, boolean forceCleanup) {
        for (Audience audience : match.audiences())
            activePlayersMap.remove((Player) audience);
        if ((currentMatchID <= MAX_MATCHES>>1) && !forceCleanup)
            match.freeze();
        else {
            match.shutdown();
            matches[match.getMatchID()] = null;
            currentMatchID = match.getMatchID()-1;
        }
    }

    public void freezeMatch(BrickballMatch match) {
        for (Audience audience : match.audiences())
            activePlayersMap.remove((Player) audience);
        match.freeze();
    }

    public boolean joinMatch(Player player, BrickballMatch match) {
        activePlayersMap.put(player, match);
        return match.joinMatch(player);
    }

    // Have a player leave their current match.
    // Returns true if it successfully removed a player from a match, false otherwise.
    public boolean leaveMatch(Player player) {
        BrickballMatch match = activePlayersMap.get(player);
        if (match == null) return false;
        activePlayersMap.remove(player);
        try {
            match.leaveMatch(player);
        } finally {
            // Freeze if no players remain
            if (((HashSet) match.audiences()).isEmpty())
                freezeMatch(match);
        }
        return true;
    }

    public boolean deepCleanWorld() {
        // don't deep clean if any matches are active
        if (getMatch(null,null) != null) return false;
        BukkitWorld bukkitWorld = new BukkitWorld(Brickball.getInstance().getMatchWorld());
        for (CuboidRegion region : WGUtils.subdivideCuboidRegion(   // Subdivide the entire match area into chunks
                new CuboidRegion(bukkitWorld,
                        new BlockVector3 (0, 100, 0),
                        new BlockVector3(ARENA_GRID_SIDE_LENGTH*ARENA_SPACING, 255, ARENA_GRID_SIDE_LENGTH*ARENA_SPACING))))
            Brickball.getInstance().getScheduler().submitTask(new RegionCleanupTask(region, bukkitWorld, Integer.MAX_VALUE));
        return true;
    }

    private Location getArenaMinPoint(int matchID) {
        int x = (matchID % ARENA_GRID_SIDE_LENGTH) * ARENA_SPACING;
        int z = (matchID / ARENA_GRID_SIDE_LENGTH) * ARENA_SPACING;
        return new Location(Brickball.getInstance().getMatchWorld(), x, 100, z);
    }
}

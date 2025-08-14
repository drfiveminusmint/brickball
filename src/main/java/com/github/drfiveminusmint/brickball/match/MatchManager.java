package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MatchManager {
    int currentMatchID = 0;
    static final int MAX_MATCHES = 255;
    private BrickballMatch[] matches = new BrickballMatch[MAX_MATCHES];
    //Technically redundant but very useful if the number of matches becomes very large
    private final ConcurrentHashMap<Player,BrickballMatch> activePlayersMap = new ConcurrentHashMap<>();

    public void stopAllMatches()
    {
        // TODO make this async
        for (BrickballMatch match : matches) {
            if (match == null) continue;
            match.shutdown();
            Brickball.getInstance().getLogger().log(Level.SEVERE, "gamer");
        }
        matches = new BrickballMatch[MAX_MATCHES];
        activePlayersMap.clear();
    }

    //TODO search criteria
    public BrickballMatch getMatch() {
        for (BrickballMatch match : matches) {
            // other logic
            return match;
        }
        return null;
    }

    @Deprecated(forRemoval = true)
    @Nullable
    public BrickballMatch auto(Player player) {
        BrickballMatch match = getMatch();
        if (!match.joinMatch(player)) return null;
        activePlayersMap.put(player,match);
        return match;
    }

    @Nullable
    public BrickballMatch getMatchByPlayer(Player player) {
        return activePlayersMap.get(player);
    }

    public @Nullable BrickballMatch startMatch(ArenaTemplate template, Location location) {
        BrickballMatch newMatch = null;
        for (int i = currentMatchID + 1; i != currentMatchID; i = (i+1) % MAX_MATCHES) {
            if (matches[i] == null) {
                newMatch = new BrickballMatch(template, location, i);
                newMatch.initialize();
                matches[i] = newMatch;
                currentMatchID = i;
                break;
            }
        }
        return newMatch;
    }
    public void endMatch(BrickballMatch match) {
        for (Audience audience : match.audiences())
            activePlayersMap.remove((Player) audience);
        match.shutdown();
        matches[match.getMatchID()] = null;
        currentMatchID = match.getMatchID();
    }

    public void joinMatch(Player player, BrickballMatch match) {
        activePlayersMap.put(player, match);
        match.joinMatch(player);
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
            // End the match if no players remain
            if (((HashSet) match.audiences()).isEmpty())
                endMatch(match);
        }
        return true;
    }
}

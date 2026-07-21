package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.util.Counter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MatchResult {
    public final int winningScore;
    public final int losingScore;
    public final HashMap<Player, Counter> kills = new HashMap<>(), deaths = new HashMap<>(), points = new HashMap<>();
    public final List<Player> winningTeam = new ArrayList<>(), losingTeam = new ArrayList<>(), leavers = new ArrayList<>();
    public MatchResult(Team winningTeam,
                       int winningScore,
                       Team losingTeam,
                       int losingScore,
                       HashSet<Player> leavers,
                       Map<Player, Counter> kills,
                       Map<Player, Counter> deaths,
                       Map<Player, Counter> points) {
        for (String entry : winningTeam.getEntries()) {
            Player p = Bukkit.getPlayer(entry);
            if (p != null) this.winningTeam.add(p);
        }
        this.winningScore = winningScore;
        this.losingScore = losingScore;
        for (String entry : losingTeam.getEntries()) {
            Player p = Bukkit.getPlayer(entry);
            if (p != null) this.losingTeam.add(p);
        }
        this.leavers.addAll(leavers);
        this.kills.putAll(kills);
        this.deaths.putAll(deaths);
        this.points.putAll(points);
    }
}

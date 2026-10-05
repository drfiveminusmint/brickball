package com.github.drfiveminusmint.brickball.events.listener;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.events.event.MatchEndEvent;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.stats.Leaderboard;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.logging.Level;

public class MatchEndListener implements Listener {
    @EventHandler
    public void onMatchEnd(MatchEndEvent event) {
        // update stats
        BrickballFormat format = event.getFormat();
        Brickball.getInstance().getFormatStats(format).updateStats(event.getResult());
        // update leaderboards
        final Leaderboard leaderboard = Brickball.getInstance().getLeaderboard(format);
        Bukkit.getScheduler().runTaskAsynchronously(Brickball.getInstance(), leaderboard::update);
    }
}

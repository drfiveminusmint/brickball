package com.github.drfiveminusmint.brickball.events.listener;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.events.event.MatchEndEvent;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.scheduling.LeaderboardUpdateTask;
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
        if (Brickball.getInstance().getLeaderboard(format) != null)
            // update leaderboards
            Brickball.getInstance().getScheduler().submitTask(
                    new LeaderboardUpdateTask(Brickball.getInstance().getLeaderboard(format)));
    }
}

package com.github.drfiveminusmint.brickball.match;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class MatchRespawnTask extends BukkitRunnable {
    private final int respawnDelay;
    private int count;
    private final BrickballMatch match;
    private final Player player;

    @Override
    public void run() {
        int timeRemaining = respawnDelay - count++;
        if (timeRemaining <= 0) {
            // clear action bar
            player.sendActionBar(Component.text(""));
            // respawn player
            if (match.getState() == MatchState.PAUSED)
                return;
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(player.getRespawnLocation());
            player.heal(20);
            player.setFoodLevel(20);
            player.setSaturation(match.getSettings().getInt(MatchSettings.Setting.SATURATION));
            player.setFireTicks(0);
            this.cancel();
        } else {
            // every second
            if (timeRemaining % 20 == 0) {
                player.sendActionBar(Component.text("Respawning in: ", NamedTextColor.RED).append(Component.text(timeRemaining / 20, NamedTextColor.YELLOW)));
            }
        }
    }

    public MatchRespawnTask(@NotNull Player player, @NotNull BrickballMatch match) {
        respawnDelay = match.getSettings().getInt(MatchSettings.Setting.RESPAWN_DELAY);
        this.match = match;
        this.player = player;
    }
}

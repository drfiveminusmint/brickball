package com.github.drfiveminusmint.brickball.match;

import com.github.drfiveminusmint.brickball.Brickball;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.Name;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MatchSettings {
    private static MatchSettings DEFAULT;
    private final Map<NamespacedKey, Object> properties = HashMap.newHashMap(Setting.keys.size());

    public MatchSettings() {
        this.properties.putAll(DEFAULT.properties);
    }

    public MatchSettings(ConfigurationSection section) {
        //TODO loading logic
        properties.put(Setting.ARROWS, section.getInt("arrows", 10));
        properties.put(Setting.POINTS_TO_WIN, section.getInt("pointsToWin", 5));
        properties.put(Setting.RESPAWN_DELAY, section.getInt("respawnDelay", 0));
        properties.put(Setting.SHOT_CLOCK, section.getInt("shotClock", -1));
        properties.put(Setting.STEAKS, section.getInt("steaks", 8));
        properties.put(Setting.TIMER, section.getInt("timer",-1));
        properties.put(Setting.SATURATION, section.getDouble("saturation", 0.0));
        properties.put(Setting.BRICK_FUMBLING, section.getBoolean("brickFumbling", false));
        properties.put(Setting.DEATH_TURNOVERS, section.getBoolean("deathTurnovers", false));
        properties.put(Setting.NATURAL_REGENERATION, section.getBoolean("naturalRegeneration", true));
        properties.put(Setting.RESPAWNING, section.getBoolean("respawning", true));
    }

    public static void loadDefault(ConfigurationSection map) {
        DEFAULT = new MatchSettings(map);
    }

    public int getInt(NamespacedKey key) {
        int result = -1;
        if (properties.get(key) instanceof Integer setting) result = setting;
        else if (DEFAULT.properties.get(key) instanceof Integer setting) result = setting;
        return result;
    }

    public boolean getBoolean(NamespacedKey key) {
        boolean result = true;
        if (properties.get(key) instanceof Boolean setting) result = setting;
        else if (DEFAULT.properties.get(key) instanceof Boolean setting) result = setting;
        return result;
    }

    @Nullable public Object get(NamespacedKey key) {
        return properties.get(key);
    }

    public void set(NamespacedKey key, Object value) {
        properties.put(key,value);
    }

    public static class Setting {
        public static final NamespacedKey ARROWS = new NamespacedKey(Brickball.getPlugin(Brickball.class), "arrows");
        public static final NamespacedKey BRICK_FUMBLING = new NamespacedKey(Brickball.getPlugin(Brickball.class), "brickFumbling");
        public static final NamespacedKey DEATH_TURNOVERS = new NamespacedKey(Brickball.getPlugin(Brickball.class), "deathTurnovers");
        public static final NamespacedKey NATURAL_REGENERATION = new NamespacedKey(Brickball.getPlugin(Brickball.class), "naturalRegeneration");
        public static final NamespacedKey RESPAWN_DELAY = new NamespacedKey(Brickball.getPlugin(Brickball.class), "respawnDelay");
        public static final NamespacedKey RESPAWNING = new NamespacedKey(Brickball.getPlugin(Brickball.class), "respawning");
        public static final NamespacedKey POINTS_TO_WIN = new NamespacedKey(Brickball.getPlugin(Brickball.class), "pointsToWin");
        public static final NamespacedKey TIMER = new NamespacedKey(Brickball.getPlugin(Brickball.class), "timer");
        public static final NamespacedKey SATURATION = new NamespacedKey(Brickball.getPlugin(Brickball.class), "saturation");
        public static final NamespacedKey SHOT_CLOCK = new NamespacedKey(Brickball.getPlugin(Brickball.class), "shotClock");
        public static final NamespacedKey STEAKS = new NamespacedKey(Brickball.getPlugin(Brickball.class), "steaks");

        public static final Set<NamespacedKey> keys = Set.of(ARROWS, BRICK_FUMBLING, DEATH_TURNOVERS, NATURAL_REGENERATION, POINTS_TO_WIN, RESPAWN_DELAY, RESPAWNING, SATURATION, SHOT_CLOCK, STEAKS, TIMER);
        @Nullable public static NamespacedKey getKey(@NotNull String string) {
            for (NamespacedKey key : keys)
                if (key.getKey().equalsIgnoreCase(string)) return key;
            return null;
        }
    }

}

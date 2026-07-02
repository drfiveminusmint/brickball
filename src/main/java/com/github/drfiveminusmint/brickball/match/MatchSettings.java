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

    private MatchSettings() {}

    public static MatchSettings cloneDefault() {
        MatchSettings result = new MatchSettings();
        result.properties.putAll(DEFAULT.properties);
        return result;
    }

    // Load the settings for a given section in the following priority order:
    // Given config section > defaults > hardcoded fallback values
    public MatchSettings(@Nullable ConfigurationSection section) {
        if (section == null) {
            this.properties.putAll(DEFAULT.properties);
            return;
        }
        properties.put(Setting.ARROWS, section.getInt("arrows", (Integer) DEFAULT.properties.getOrDefault(Setting.ARROWS, 10)));
        properties.put(Setting.POINTS_TO_WIN, section.getInt("pointsToWin", (Integer) DEFAULT.properties.getOrDefault(Setting.POINTS_TO_WIN, 5)));
        properties.put(Setting.RESPAWN_DELAY, section.getInt("respawnDelay", (Integer) DEFAULT.properties.getOrDefault(Setting.RESPAWN_DELAY, 0)));
        properties.put(Setting.SHOT_CLOCK, section.getInt("shotClock", (Integer) DEFAULT.properties.getOrDefault(Setting.SHOT_CLOCK, -1)));
        properties.put(Setting.STEAKS, section.getInt("steaks", (Integer) DEFAULT.properties.getOrDefault(Setting.STEAKS, 8)));
        properties.put(Setting.TIMER, section.getInt("timer",(Integer) DEFAULT.properties.getOrDefault(Setting.TIMER, -1)));
        properties.put(Setting.SATURATION, section.getDouble("saturation", (Double) DEFAULT.properties.getOrDefault(Setting.SATURATION, 0.0)));
        properties.put(Setting.BRICK_FUMBLING, section.getBoolean("brickFumbling", (Boolean) DEFAULT.properties.getOrDefault(Setting.BRICK_FUMBLING, false)));
        properties.put(Setting.DEATH_TURNOVERS, section.getBoolean("deathTurnovers", (Boolean) DEFAULT.properties.getOrDefault(Setting.DEATH_TURNOVERS, false)));
        properties.put(Setting.NATURAL_REGENERATION, section.getBoolean("naturalRegeneration", (Boolean) DEFAULT.properties.getOrDefault(Setting.NATURAL_REGENERATION, true)));
        properties.put(Setting.RESPAWNING, section.getBoolean("respawning", (Boolean) DEFAULT.properties.getOrDefault(Setting.RESPAWNING, true)));
    }

    public static void loadDefault(ConfigurationSection map) {
        // load defaults or get fallback values
        DEFAULT = new MatchSettings(); // DON'T REMOVE THIS, IT PREVENTS A NPE, YES I KNOW IT'S DUMB
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

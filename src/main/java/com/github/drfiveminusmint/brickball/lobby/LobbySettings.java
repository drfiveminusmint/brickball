package com.github.drfiveminusmint.brickball.lobby;

import com.github.drfiveminusmint.brickball.Brickball;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Set;

public class LobbySettings {
    private final HashMap<NamespacedKey, Object> properties = new HashMap<>();

    private static LobbySettings DEFAULT;
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

    @Nullable
    public Object get(NamespacedKey key) {
        return properties.get(key);
    }

    public void set(NamespacedKey key, Object value) {
        properties.put(key,value);
    }
    public static class Setting {
        public static final NamespacedKey PRIVATE = new NamespacedKey(Brickball.getPlugin(Brickball.class), "private");


        public static final Set<NamespacedKey> keys = Set.of();
        @Nullable public static NamespacedKey getKey(@NotNull String string) {
            for (NamespacedKey key : keys)
                if (key.getKey().equalsIgnoreCase(string)) return key;
            return null;
        }
    }
}

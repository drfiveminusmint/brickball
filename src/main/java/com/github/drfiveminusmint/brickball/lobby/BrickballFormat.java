package com.github.drfiveminusmint.brickball.lobby;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class BrickballFormat {
    private final String name;
    private final boolean isRated;
    private final int minPlayersPerTeam, maxPlayersPerTeam;
    private final MatchSettings settings;
    private final List<ArenaTemplate> validMaps;

    public BrickballFormat(ConfigurationSection section) {
        this.name = section.getString("name");
        if (name == null)
            throw new IllegalArgumentException("All Brickball formats must have 'name' defined.");
        this.minPlayersPerTeam = section.getInt("minPlayersPerTeam", 0);
        this.maxPlayersPerTeam = section.getInt("maxPlayersPerTeam", 99);
        this.isRated = section.getBoolean("isRated", false);
        // if there is no such section, this will return a clone of the default settings
        this.settings = new MatchSettings(section.getConfigurationSection("overrideSettings"));
        // an empty map list indicates all maps are allowed
        validMaps = new ArrayList<>();
        for (String s : section.getStringList("maps")) {
            ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(s);
            if (template != null)
                validMaps.add(template);
            else
                Brickball.getInstance().getLogger().log(Level.WARNING, String.format("Cannot find map '%s' for format '%s'.", s, name));
        }
    }

    public String getName() { return name; }

    public boolean getIsRated() {return isRated;}

    public int getMinPlayersPerTeam() { return minPlayersPerTeam; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }

    public MatchSettings getSettings() { return settings; }

    public Collection<ArenaTemplate> getValidMaps() {
        if (validMaps.isEmpty()) return Brickball.getInstance().getTemplateManager().templates.values();
        return validMaps;
    }
}

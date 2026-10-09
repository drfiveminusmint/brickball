package com.github.drfiveminusmint.brickball.lobby;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.fiveUI.util.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class BrickballFormat {
    private final String name;
    private final boolean isRated, doMatchmaking;
    private final int minPlayersPerTeam, maxPlayersPerTeam;
    private final MatchSettings settings;
    private final List<ArenaTemplate> validMaps;
    private final String requiredPermission;
    private final ItemStack inactiveDisplayItem, activeDisplayItem;

    public BrickballFormat(ConfigurationSection section) {
        this.name = section.getString("name");
        if (name == null)
            throw new IllegalArgumentException("All Brickball formats must have 'name' defined.");
        this.minPlayersPerTeam = section.getInt("minPlayersPerTeam", 0);
        this.maxPlayersPerTeam = section.getInt("maxPlayersPerTeam", 99);
        this.isRated = section.getBoolean("isRated", false);
        this.doMatchmaking = section.getBoolean("doMatchmaking", false);
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
        this.requiredPermission = section.getString("requiredPermission", "");
        ConfigurationSection itemSection = section.getConfigurationSection("displayItem");
        // Create the 'selected' and 'unselected' items for the lobby creation GUI
        // The selected item has an enchantment glow.
        ItemStackBuilder builder;
        if (itemSection == null) {
            builder = new ItemStackBuilder(Material.BRICK, 1);
        } else {
            builder = new ItemStackBuilder(Material.valueOf(itemSection.getString("type", "BRICK")), itemSection.getInt("quantity", 1))
                    .addLore(Component.text(itemSection.getString("lore", "")).decoration(TextDecoration.ITALIC, false));
        }
        this.inactiveDisplayItem = builder.name(Component.text(name, (this.isRated ? NamedTextColor.RED : NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false)).itemStack();
        this.activeDisplayItem = builder.name(Component.text(name, (this.isRated ? NamedTextColor.RED : NamedTextColor.AQUA), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .setGlimmer(true).itemStack();
    }

    public String getName() { return name; }

    public boolean getIsRated() { return isRated; }

    public boolean getDoMatchmaking() { return doMatchmaking; }

    public int getMinPlayersPerTeam() { return minPlayersPerTeam; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }

    public MatchSettings getSettings() { return settings; }

    public Collection<ArenaTemplate> getValidMaps() {
        if (validMaps.isEmpty()) return Brickball.getInstance().getTemplateManager().templates.values();
        return validMaps;
    }

    public String getRequiredPermission() { return requiredPermission; }

    public ItemStack getUnselectedDisplayItem() { return inactiveDisplayItem; }
    public ItemStack getSelectedDisplayItem() { return activeDisplayItem; }
}

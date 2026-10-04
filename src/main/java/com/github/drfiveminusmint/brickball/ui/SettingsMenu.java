package com.github.drfiveminusmint.brickball.ui;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.lobby.Lobby;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.fiveUI.FiveUI;
import com.github.drfiveminusmint.fiveUI.container.Page;
import com.github.drfiveminusmint.fiveUI.container.TextInput;
import com.github.drfiveminusmint.fiveUI.element.*;
import com.github.drfiveminusmint.fiveUI.util.ItemStackBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class SettingsMenu {
    private final Lobby lobby;
    private final Page mainPage, mapSelectionPage;
    public SettingsMenu(Lobby linkedLobby) {
        lobby = linkedLobby;
        // Create GUI
        mainPage = new Page(Component.text("Match Settings", NamedTextColor.DARK_GRAY, TextDecoration.BOLD), InventoryType.CHEST);
        // Create toggle buttons for the boolean match settings
        mainPage.setElement(0, createToggleButton(MatchSettings.Setting.BRICK_FUMBLING, "Brick Fumbling", Material.NETHER_BRICK, Material.BRICK));
        mainPage.setElement(1, createToggleButton(MatchSettings.Setting.DEATH_TURNOVERS, "Death Turnovers", Material.CHEST, Material.ENDER_CHEST));
        mainPage.setElement(2, createToggleButton(MatchSettings.Setting.NATURAL_REGENERATION, "Natural Regeneration", Material.GOLDEN_APPLE, Material.GLASS_BOTTLE));
        mainPage.setElement(3, createToggleButton(MatchSettings.Setting.RESPAWNING, "Respawning", Material.TOTEM_OF_UNDYING, Material.SKELETON_SKULL));
        // Create number prompt buttons for the integer match settings
        mainPage.setElement(18, createNumberPromptButton(MatchSettings.Setting.ARROWS, mainPage, "Arrows", Material.ARROW, 1));
        mainPage.setElement(19, createNumberPromptButton(MatchSettings.Setting.STEAKS, mainPage, "Steaks", Material.COOKED_BEEF, 1));
        mainPage.setElement(20, createNumberPromptButton(MatchSettings.Setting.RESPAWN_DELAY, mainPage, "Respawn Delay (Seconds)", Material.SPAWNER, 20));
        mainPage.setElement(21, createNumberPromptButton(MatchSettings.Setting.SHOT_CLOCK, mainPage, "Shot Clock (Seconds)", Material.TNT, 1));
        mainPage.setElement(22, createNumberPromptButton(MatchSettings.Setting.TIMER, mainPage, "Match Time (Minutes)", Material.CLOCK, 60));
        mainPage.setElement(23, createNumberPromptButton(MatchSettings.Setting.POINTS_TO_WIN, mainPage, "Points to win", Material.GREEN_STAINED_GLASS, 1));

        // Create map selection page
        mapSelectionPage = new Page(Component.text("Select Map", NamedTextColor.DARK_GRAY, TextDecoration.BOLD), InventoryType.CHEST);
        int mapsFound = 0;
        for (ArenaTemplate map : lobby.getFormat().getValidMaps()) {
            // possible TODO: map-specific button icons?
            RadioButton button = new RadioButton(
                    new ItemStackBuilder(Material.NETHER_BRICK, 1)
                            .name(Component.text(map.getID(), NamedTextColor.GRAY))
                            .itemStack(),
                    new ItemStackBuilder(Material.BRICK, 1)
                            .name(Component.text(map.getID(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .itemStack()
            );
            // Link all maps after the first with the first button
            if (mapsFound != 0)
                button.link((RadioButton) mapSelectionPage.getElement(0));
            // Initialize the button corresponding to the current map as clicked
            if (map.equals(lobby.getNextMap()))
                button.setClicked();
            // When this button is clicked, set the next map to the corresponding ArenaTemplate
            button.setOnClick(((player, clickableElement, clickType) -> lobby.setMap(map)));
            mapSelectionPage.setElement(mapsFound++, button);
            // Prevent overflow
            if (mapsFound >= mapSelectionPage.getInventory().getSize())
                break;
        }
        // return to main page on closing
        mapSelectionPage.setOnClose(((player, container) -> Bukkit.getScheduler().runTaskLater(Brickball.getInstance(), () -> mainPage.display(player), 1)));

        // Link main page to map selection page
        mainPage.setElement(8, new LinkButton(
                new ItemStackBuilder(Material.GRASS_BLOCK, 1)
                        .name(Component.text("Select next map...", NamedTextColor.AQUA, TextDecoration.ITALIC))
                        .itemStack(),
                mapSelectionPage
        ));
    }

    public void destroy() {
        FiveUI.getInstance().getUIManager().unregisterInterface(mainPage);
    }

    public void open(Player player) {
        mainPage.display(player);
    }

    private SelectorButton createToggleButton(NamespacedKey key, String humanName, Material trueMaterial, Material falseMaterial) {
        SelectorButton result = new SelectorButton(new ItemStack[] {
                new ItemStackBuilder(falseMaterial, 1)
                        .name(Component.text(humanName, NamedTextColor.GRAY))
                        .addLore(Component.text("Disabled"))
                        .itemStack(),
                new ItemStackBuilder(trueMaterial, 1)
                        .name(Component.text(humanName, NamedTextColor.AQUA, TextDecoration.BOLD))
                        .addLore(Component.text("Enabled"))
                        .itemStack()
        });
        if ((Boolean) lobby.getMatchSetting(key))
            result.setState(1);
        result.setOnEntry(((player, o) -> lobby.setMatchSetting(key, (Integer) o == 1)));
        return result;
    }

    private DynamicButton createNumberPromptButton(NamespacedKey key, Page mainPage, String humanName, Material material, int displayDivisor) {
        DynamicButton result = new DynamicButton(() -> {
            int displayQuantity = (int) lobby.getMatchSetting(key) / displayDivisor;
            if (displayQuantity < 1 || displayQuantity > 64)
                displayQuantity = 1;
            return new ItemStackBuilder(material, displayQuantity)
                    .name(Component.text(humanName, NamedTextColor.YELLOW))
                    .addLore(Component.text("Click to edit"))
                    .itemStack();
        });
        result.setOnClick(((player, clickableElement, clickType) -> {
            TextInput numberInput = new TextInput(Component.text(humanName, NamedTextColor.YELLOW, TextDecoration.BOLD), player);
            numberInput.setOnClose((player1, container) -> {
                // destroy the interface to prevent resource leak
                FiveUI.getInstance().getUIManager().unregisterInterface(numberInput);
                // return to main page
                Bukkit.getScheduler().runTaskLater(Brickball.getInstance(), () -> mainPage.display(player), 1);
            });
            numberInput.setElement(0, new StaticDisplay(
                    new ItemStackBuilder(material, 1)
                    .name(Component.text("0"))
                    .itemStack()));
            // set the match setting to the number the user entered
            // fractional values are supported in the input but are floored
            numberInput.setOnEntry((player1, o) -> {
                if (o == null) {
                    return;
                } try {
                    double raw = Double.parseDouble((String) o);
                    lobby.setMatchSetting(key, (int) (raw * displayDivisor));
                    mainPage.updateContents();
                    player1.closeInventory();
                    // There's no easy way to tell the player that they entered the number incorrectly here, unfortunately
                } catch (NumberFormatException exception) { player.playSound(Sound.sound(Key.key("entity.blaze.death"), Sound.Source.BLOCK, 1f, 1f)); }
            });
            numberInput.display(player);
        }));
        return result;
    }
}

package com.github.drfiveminusmint.brickball.ui;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.Lobby;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.fiveUI.FiveUI;
import com.github.drfiveminusmint.fiveUI.container.Page;
import com.github.drfiveminusmint.fiveUI.container.TextInput;
import com.github.drfiveminusmint.fiveUI.element.DynamicButton;
import com.github.drfiveminusmint.fiveUI.element.SelectorButton;
import com.github.drfiveminusmint.fiveUI.element.StaticDisplay;
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

import java.util.logging.Level;

public class SettingsMenu {
    private final Lobby lobby;
    private final Page page;
    public SettingsMenu(Lobby linkedLobby) {
        lobby = linkedLobby;
        // Create GUI
        page = new Page(Component.text("Match Settings", NamedTextColor.DARK_GRAY, TextDecoration.BOLD), InventoryType.CHEST);
        // Create toggle buttons for the boolean match settings
        page.setElement(0, createToggleButton(MatchSettings.Setting.BRICK_FUMBLING, "Brick Fumbling", Material.NETHER_BRICK, Material.BRICK));
        page.setElement(1, createToggleButton(MatchSettings.Setting.DEATH_TURNOVERS, "Death Turnovers", Material.CHEST, Material.ENDER_CHEST));
        page.setElement(2, createToggleButton(MatchSettings.Setting.NATURAL_REGENERATION, "Natural Regeneration", Material.GOLDEN_APPLE, Material.GLASS_BOTTLE));
        page.setElement(3, createToggleButton(MatchSettings.Setting.RESPAWNING, "Respawning", Material.TOTEM_OF_UNDYING, Material.SKELETON_SKULL));
        // Create number prompt buttons for the integer match settings
        page.setElement(18, createNumberPromptButton(MatchSettings.Setting.ARROWS, page, "Arrows", Material.ARROW, 1));
        page.setElement(19, createNumberPromptButton(MatchSettings.Setting.STEAKS, page, "Steaks", Material.COOKED_BEEF, 1));
        page.setElement(20, createNumberPromptButton(MatchSettings.Setting.RESPAWN_DELAY, page, "Respawn Delay (Seconds)", Material.RED_BED, 20));
        page.setElement(21, createNumberPromptButton(MatchSettings.Setting.SHOT_CLOCK, page, "Shot Clock (Seconds)", Material.TNT, 20));
        page.setElement(22, createNumberPromptButton(MatchSettings.Setting.TIMER, page, "Match Time (Minutes)", Material.CLOCK, 60));
        page.setElement(23, createNumberPromptButton(MatchSettings.Setting.POINTS_TO_WIN, page, "Points to win", Material.GREEN_STAINED_GLASS, 1));
    }

    public void destroy() {
        FiveUI.getInstance().getUIManager().unregisterInterface(page);
    }

    public void open(Player player) {
        page.display(player);
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
        // TODO REMOVE THIS
        result.setOnEntry(((player, o) -> {}));
        if ((Boolean) lobby.getMatchSetting(key))
            result.onClick(null, ClickType.LEFT); // horrible hack, TODO fix this when the API is updated
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

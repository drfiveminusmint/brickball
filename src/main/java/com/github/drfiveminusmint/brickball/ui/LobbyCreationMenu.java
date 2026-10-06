package com.github.drfiveminusmint.brickball.ui;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.lobby.Lobby;
import com.github.drfiveminusmint.fiveUI.FiveUI;
import com.github.drfiveminusmint.fiveUI.container.Page;
import com.github.drfiveminusmint.fiveUI.element.RadioButton;
import com.github.drfiveminusmint.fiveUI.element.SelectorButton;
import com.github.drfiveminusmint.fiveUI.element.StaticButton;
import com.github.drfiveminusmint.fiveUI.util.ItemStackBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class LobbyCreationMenu {
    Player host;
    BrickballFormat format;
    boolean isPrivate;

    public LobbyCreationMenu(Player creator) {
        host = creator;
        // Create main page
        Page mainPage = new Page(Component.text("Create Custom Match"), InventoryType.CHEST);
        mainPage.setOnClose((player, container) -> FiveUI.getInstance().getUIManager().unregisterInterface(container));
        // Add buttons for all formats to the page
        int formatsFound = 0;
        for (BrickballFormat candidate : Brickball.getInstance().getFormats()) {
            if (candidate.getRequiredPermission().equalsIgnoreCase("")
                    || host.hasPermission(candidate.getRequiredPermission())) {
                RadioButton button = new RadioButton(candidate.getUnselectedDisplayItem(), candidate.getSelectedDisplayItem());
                button.setOnClick(((player, clickableElement, clickType) -> format = candidate));
                if (formatsFound != 0)
                    button.link((RadioButton) mainPage.getElement(formatsFound-1));
                mainPage.setElement(formatsFound++, button);

            }
        }
        // Privacy button
        SelectorButton privateButton = new SelectorButton(new org.bukkit.inventory.ItemStack[] {
                new ItemStackBuilder(Material.ENDER_EYE, 1)
                        .name(Component.text("Public Lobby", NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .addLore(Component.text("Other players will be able to see and join this lobby."))
                        .itemStack(),
                new ItemStackBuilder(Material.ENDER_PEARL, 1)
                        .name(Component.text("Private Lobby", NamedTextColor.YELLOW, TextDecoration.BOLD))
                        .addLore(Component.text("Other players will not be able to join this lobby. Use /brickball invite."))
                        .itemStack()
        });
        privateButton.setOnEntry(((player, o) -> isPrivate = ((Integer) o) == 1));
        mainPage.setElement(18, privateButton);

        // Creation button
        StaticButton createButton = new StaticButton(new ItemStackBuilder(Material.GREEN_CONCRETE, 1)
                .name(Component.text("Create Match!", NamedTextColor.GREEN, TextDecoration.BOLD))
                .itemStack());
        createButton.setOnClick(((player, clickableElement, clickType) -> {
            if (format == null)
                return;
            build();
            host.closeInventory();
        }));
        mainPage.setElement(26, createButton);

        // Display
        mainPage.display(host);
    }

    public void build() {
        Lobby newLobby = new Lobby(format, isPrivate);
        if (!isPrivate)
            Bukkit.getServer().broadcast(Component.text("[Brickball] ", NamedTextColor.GOLD)
                    .append(host.displayName())
                    .append(Component.text(" has created a ", NamedTextColor.GOLD))
                    .append(Component.text(format.getName(), NamedTextColor.YELLOW)).append(Component.text(" lobby. ", NamedTextColor.GOLD))
                    .append(Component.text("Click to join!", NamedTextColor.AQUA)).clickEvent(ClickEvent.runCommand("/brickball join " + host.getName())));
        newLobby.invite(host, null);
        newLobby.join(host, 2); // start as spectator
        newLobby.setHost(host);
        host.sendMessage(Component.text("[Start Match]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball start")));


    }
}

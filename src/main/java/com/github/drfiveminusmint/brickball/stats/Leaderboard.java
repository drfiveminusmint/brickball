package com.github.drfiveminusmint.brickball.stats;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.fiveUI.container.Page;
import com.github.drfiveminusmint.fiveUI.element.DynamicButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.LinkedList;
import java.util.List;

public class Leaderboard {
    private static final int MAX_SIZE = 27;
    private final OfflinePlayer[] top = new OfflinePlayer[MAX_SIZE];
    private final Page displayPage;
    private final BrickballFormat brickballFormat;

    public Leaderboard(BrickballFormat format) {
        brickballFormat = format;
        displayPage = new Page(Component.text("Leaderboard for " + format.getName(),
                NamedTextColor.GOLD,
                TextDecoration.BOLD), InventoryType.CHEST);
        for (int i = 0; i < MAX_SIZE; i++) {
            final int index = i;
            DynamicButton button = new DynamicButton(() -> {
                if (top[index] == null)
                    return null;
                ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta headMeta = (SkullMeta) head.getItemMeta();
                headMeta.setOwningPlayer(top[index]);
                headMeta.displayName(Component.text(
                        String.format("#%d: %s (%d)", index+1, top[index].getName(),
                                Brickball.getInstance().getFormatStats(format).getPlayerStat(top[index], FormatStats.TrackedStat.RATING)),
                        NamedTextColor.YELLOW, TextDecoration.BOLD));
                headMeta.lore(List.of(Component.text("Click to see full stats")));
                head.setItemMeta(headMeta);
                return head;
            });
            button.setOnClick(((player, clickableElement, clickType) -> {
                Brickball.getInstance().getFormatStats(format).displayStats(player, top[index]);
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            }));
            displayPage.setElement(i, button);
        }
    }

    public Page getDisplayPage() {
        return displayPage;
    }

    // Updates the leaderboard
    // Expensive, so only run this after a game, and try to run it asynchronously
    public void update() {
        LinkedList<OfflinePlayer> list = new LinkedList<>();
        for (OfflinePlayer player : Brickball.getInstance().getFormatStats(brickballFormat).getPlayers()) {
            int pRating = Brickball.getInstance().getFormatStats(brickballFormat).getPlayerStat(player, FormatStats.TrackedStat.RATING);
            for(int i = 0; i < MAX_SIZE; i++) {
                if (list.size() <= i || Brickball.getInstance().getFormatStats(brickballFormat)
                        .getPlayerStat(list.get(i), FormatStats.TrackedStat.RATING) < pRating) {
                    list.add(i, player); // insert player here in the list and shift all others right
                    break;
                }
            }
            // copy to our top players
            // asynchronously modifying top[] probably doesn't matter here, because we're updating it next tick anyway
            for(int i = 0; i < MAX_SIZE && i < list.size(); i++)
                top[i] = list.get(i);
        }
        // This has to be run in the main thread
        Bukkit.getScheduler().runTask(Brickball.getInstance(), displayPage::updateContents);
    }
}

package com.github.drfiveminusmint.brickball.util;

import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class BrickballColor  {
    public final Color bukkitColor;
    public final NamedTextColor textColor;
    public final BaseBlock glassType;
    public static final BrickballColor BLACK = new BrickballColor(Color.BLACK, NamedTextColor.BLACK, BlockTypes.BLACK_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor BLUE = new BrickballColor(Color.BLUE, NamedTextColor.BLUE, BlockTypes.BLUE_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor CYAN = new BrickballColor(Color.TEAL, NamedTextColor.DARK_AQUA, BlockTypes.CYAN_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor GRAY = new BrickballColor(Color.GRAY, NamedTextColor.DARK_GRAY, BlockTypes.GRAY_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor GREEN = new BrickballColor(Color.GREEN, NamedTextColor.DARK_GREEN, BlockTypes.GREEN_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor LIGHT_BLUE = new BrickballColor(Color.AQUA, NamedTextColor.AQUA, BlockTypes.CYAN_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor LIGHT_GRAY = new BrickballColor(Color.SILVER, NamedTextColor.GRAY, BlockTypes.LIGHT_GRAY_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor LIME = new BrickballColor(Color.LIME, NamedTextColor.GREEN, BlockTypes.GREEN_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor MAGENTA = new BrickballColor(Color.FUCHSIA, NamedTextColor.LIGHT_PURPLE, BlockTypes.MAGENTA_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor ORANGE = new BrickballColor(Color.ORANGE, NamedTextColor.GOLD, BlockTypes.ORANGE_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    // WHY THE FUCK IS THIS NOT A NAMED COLOR
    public static final BrickballColor PINK = new BrickballColor(Color.fromRGB(0xB6687F), NamedTextColor.LIGHT_PURPLE, BlockTypes.PINK_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor PURPLE = new BrickballColor(Color.PURPLE, NamedTextColor.DARK_PURPLE, BlockTypes.PURPLE_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor RED = new BrickballColor(Color.RED, NamedTextColor.RED, BlockTypes.RED_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor WHITE = new BrickballColor(Color.WHITE, NamedTextColor.WHITE, BlockTypes.WHITE_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());
    public static final BrickballColor YELLOW = new BrickballColor(Color.YELLOW, NamedTextColor.YELLOW, BlockTypes.YELLOW_STAINED_GLASS_PANE.getDefaultState().toBaseBlock());

    private BrickballColor(Color a, NamedTextColor b, BaseBlock c) {bukkitColor = a; textColor = b; glassType = c;}

    public static @Nullable BrickballColor getNamedColor(String alias) {
        if (alias.equalsIgnoreCase("black")) return BLACK;
        if (alias.equalsIgnoreCase("blue") || alias.equalsIgnoreCase("darkblue")) return BLUE;
        if (alias.equalsIgnoreCase("cyan") || alias.equalsIgnoreCase("teal")) return CYAN;
        if (alias.equalsIgnoreCase("gray") || alias.equalsIgnoreCase("grey") || alias.equalsIgnoreCase("darkgray") || alias.equalsIgnoreCase("darkgrey")) return GRAY;
        if (alias.equalsIgnoreCase("green") || alias.equalsIgnoreCase("darkgreen")) return GREEN;
        if (alias.equalsIgnoreCase("lightblue") || alias.equalsIgnoreCase("aqua")) return LIGHT_BLUE;
        if (alias.equalsIgnoreCase("lightgray") || alias.equalsIgnoreCase("lightgrey")) return LIGHT_GRAY;
        if (alias.equalsIgnoreCase("lime") || alias.equalsIgnoreCase("limegreen") || alias.equalsIgnoreCase("lightgreen")) return LIME;
        if (alias.equalsIgnoreCase("magenta") || alias.equalsIgnoreCase("lightpurple")) return MAGENTA;
        if (alias.equalsIgnoreCase("orange") || alias.equalsIgnoreCase("gold")) return ORANGE;
        if (alias.equalsIgnoreCase("pink")) return PINK;
        if (alias.equalsIgnoreCase("purple") || alias.equalsIgnoreCase("darkpurple")) return PURPLE;
        if (alias.equalsIgnoreCase("red")) return RED;
        if (alias.equalsIgnoreCase("white")) return WHITE;
        if (alias.equalsIgnoreCase("yellow")) return YELLOW;
        return null;
    }
}

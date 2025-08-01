package com.github.drfiveminusmint.brickball.command;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BrickballCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("Usage: /brickball (join/jointeam/create/leave/map/start)");
            return true;
        }
        if (args[0].equalsIgnoreCase("join")) return joinCommand(player, args);
        if (args[0].equalsIgnoreCase("create")) return createCommand(player, args);
        if (args[0].equalsIgnoreCase("leave")) return leaveCommand(player);
        if (args[0].equalsIgnoreCase("jointeam")) return teamCommand(player, args);
        if (args[0].equalsIgnoreCase("map")) return mapCommand(player, args);
        if (args[0].equalsIgnoreCase("start")) return startCommand(player);
        if (args[0].equalsIgnoreCase("teamColor")) return teamColorCommand(player, args);
        return false;
    }

    public boolean teamColorCommand (Player player, String[] args) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage("You're not in a Brickball match.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Usage: /brickball teamcolor (1/2) (color)");
            return true;
        }
        int team;
        try {
            team = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            team = 0;
        }
        if (team < 1 || team > 2) {
            player.sendMessage("Usage: /brickball teamcolor (1/2) (color)");
            return true;
        }
        BrickballColor color = BrickballColor.getNamedColor(args[2].replaceAll("_", ""));
        if (color == null) {
            player.sendMessage("Error: Cannot find color " + args[2]);
            return true;
        }
        // the internal array is zero-indexed but that confuses users, so we have to convert
        match.setTeamColor(color, team-1);
        return true;
    }

    public boolean createCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball create (map)");
            return true;
        }
        ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(args[1]);
        if (template == null) {
            player.sendMessage(String.format("Couldn't find arena template: %s", args[1]));

        }
        BrickballMatch newMatch = Brickball.getInstance().getMatchManager().startMatch(template, player.getLocation());
        if (newMatch == null) {
            player.sendMessage("Error creating match!");
            return true;
        }
        Brickball.getInstance().getMatchManager().joinMatch(player, newMatch);
        return true;
    }

    public boolean joinCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball join (user)");
            return true;
        }
        Player otherPlayer = Bukkit.getPlayer(args[1]);
        if (otherPlayer == null) {
            player.sendMessage(String.format("Player %s not found.", args[1]));
            return true;
        }
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(otherPlayer);
        if (match == null) {
            player.sendMessage(String.format("Player %s has no active Brickball match you can join.", args[1]));
            return true;
        }
        Brickball.getInstance().getMatchManager().joinMatch(player, match);
        return true;
    }

    public boolean leaveCommand(Player player) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage("You're not in a Brickball match.");
            return true;
        }
        Brickball.getInstance().getMatchManager().leaveMatch(player, match);
        return true;
    }

    public boolean teamCommand(Player player, String[] args) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage("You're not in a Brickball match.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("Specify a team to join (Spectator is team 3).");
            return true;
        }
        try {
            if (!match.joinTeam(player, Integer.parseInt(args[1]) - 1)) {
                player.sendMessage("Error joining team.");
                return true;
            }
        } catch (Exception e) {
            player.sendMessage("Error joining team.");
            return true;
        }
        return true;
    }

    public boolean mapCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball map (create/list)");
            return true;
        }
        if (args[1].equalsIgnoreCase("list")) {
            for (ArenaTemplate template : Brickball.getInstance().getTemplateManager().templates.values())
                player.sendMessage(Component.text(template.getID()));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Usage: /brickball map create (id)");
            return true;
        }
        if (args[1].equalsIgnoreCase("create")) {
            TemplateManager manager = Brickball.getInstance().getTemplateManager();
            ArenaTemplate newTempate;
            try {
                newTempate = ArenaTemplate.createByID(args[2], player.getLocation(), new BukkitWorld(player.getWorld()));
            } catch (Exception ex) {
                player.sendMessage("Error creating map.");
                return true;
            }
            if (manager.findTemplate(args[2]) != null)
                manager.deleteTemplate(args[2]);
            manager.registerTemplate(newTempate);
            player.sendMessage("Map creation successful!");
            return true;
        }
        player.sendMessage("Usage: /brickball map (create/list)");
        return true;
    }

    public boolean startCommand(Player player) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage("You're not in a Brickball match.");
            return true;
        }
        if (match.startMatch()) {
            player.sendMessage("Match starting! Please wait...");
        } else {
            player.sendMessage("Error starting match.");
        }
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (args[0].equalsIgnoreCase("join")) {
            for (Player player : Bukkit.getServer().getOnlinePlayers())
                result.add(player.getName());
            return result;
        }
        if (args[0].equalsIgnoreCase("create")) return Brickball.getInstance().getTemplateManager().listTemplateIDs();
        if (args[0].equalsIgnoreCase("map")) return List.of("create", "list");
        if (args[0].equalsIgnoreCase("jointeam")) return List.of("1", "2", "3");
        if (args[0].equalsIgnoreCase("teamColor")) {
            if (args.length == 2) return List.of("1", "2");
            return List.of("black", "blue", "cyan", "gray", "green", "lightblue","lightgray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");
        }
        if (args.length == 1) return List.of("create", "join", "jointeam", "leave", "map", "start", "teamcolor");
        return null;
    }
}

package com.github.drfiveminusmint.brickball.command;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchManager;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
        if (args[0].equalsIgnoreCase("setting")) return settingCommand(player, args);
        if (args[0].equalsIgnoreCase("setworld")) return setWorldCommand(player, args);
        if (args[0].equalsIgnoreCase("admin")) return adminCommand(player, args);
        return false;
    }

    public boolean adminCommand (Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball admin (cleanworld/shutdown/reload)");
            return true;
        }
        if (args[1].equalsIgnoreCase("cleanworld")) {
            if (Brickball.getInstance().getMatchManager().getMatch(null,null) != null) {
                player.sendMessage("Some matches are currently running, please run \"/brickball admin shutdown\" first");
                return true;
            }
            if (!Brickball.getInstance().getMatchManager().deepCleanWorld())
                player.sendMessage("Something went wrong deep-cleaning the world. Did you run \"/brickball admin shutdown\" first?");
        }
        if (args[1].equalsIgnoreCase("shutdown")) {
            Brickball.getInstance().getMatchManager().stopAllMatches();
        }
        return true;
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
        if (!player.hasPermission("brickball.create")) return insufficientPermissions(player);
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball create (map)");
            return true;
        }
        if (Brickball.getInstance().getMatchManager().getMatchByPlayer(player) != null) {
            player.sendMessage("You're already in a Brickball match.");
            return true;
        }
        if (Brickball.getInstance().getMatchWorld() == null)
        {
            player.sendMessage("Error: Match world not set. Set it with /brickball setWorld");
        }
        ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(args[1]);
        if (template == null) {
            player.sendMessage(String.format("Couldn't find arena template: %s", args[1]));

        }
        BrickballMatch newMatch = Brickball.getInstance().getMatchManager().startMatch(template, 10);
        if (newMatch == null) {
            player.sendMessage("Error creating match!");
            return true;
        }
        Brickball.getInstance().getMatchManager().joinMatch(player, newMatch);
        return true;
    }

    public boolean joinCommand(Player player, String[] args) {
        if (!player.hasPermission("brickball.join")) return insufficientPermissions(player);
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
        if (Brickball.getInstance().getMatchManager().leaveMatch(player)) {
            player.sendMessage("You have left the match.");
            return true;
        }
        player.sendMessage("You're not in a Brickball match.");
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

    public boolean settingCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball setting (setting) <value>");
            return true;
        }
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage("You're not in a Brickball match.");
            return true;
        }
        NamespacedKey key = MatchSettings.Setting.getKey(args[1]);
        if (key == null) {
            player.sendMessage("Unknown match setting: " + args[1]);
            return true;
        }
        Object value = match.getSettings().get(key);
        if (args.length == 2) {
            player.sendMessage(String.format("Match setting %s is set to: %s", key.getKey(), value));
            return true;
        }
        switch (value) {
            case Integer integer -> {
                try {
                    int i = Integer.parseInt(args[2]);
                    match.getSettings().set(key, i);
                } catch (NumberFormatException exception) {
                    player.sendMessage("Could not parse argument: " + args[2]);
                    return true;
                }
            }
            case Double v -> {
                try {
                    double d = Double.parseDouble(args[2]);
                    match.getSettings().set(key, d);
                } catch (NumberFormatException exception) {
                    player.sendMessage("Could not parse argument: " + args[2]);
                    return true;
                }
            }
            case Boolean aBoolean -> {
                try {
                    boolean b = Boolean.parseBoolean(args[2]);
                    match.getSettings().set(key, b);
                } catch (NumberFormatException exception) {
                    player.sendMessage("Could not parse argument: " + args[2]);
                    return true;
                }
            }
            case null, default -> {
                player.sendMessage("Couldn't set match setting. This usually indicates a bug in the plugin. Contact the developer and show them this error:");
                player.sendMessage("Couldn't edit match setting of type " + value.getClass().getName());
                return true;
            }
        }
        player.sendMessage("Match setting has been set!");
        return true;
    }

    public boolean mapCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball map (save/list)");
            return true;
        }
        if (args[1].equalsIgnoreCase("list")) {
            if (!player.hasPermission("brickball.map.list")) return insufficientPermissions(player);
            for (ArenaTemplate template : Brickball.getInstance().getTemplateManager().templates.values())
                player.sendMessage(Component.text(template.getID()));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Usage: /brickball map create (id)");
            return true;
        }
        if (args[1].equalsIgnoreCase("save")) {
            if (!player.hasPermission("brickball.map.save")) return insufficientPermissions(player);
            TemplateManager manager = Brickball.getInstance().getTemplateManager();
            ArenaTemplate newTempate;
            try {
                newTempate = ArenaTemplate.createByID(args[2], player.getLocation(), new BukkitWorld(player.getWorld()), true);
            } catch (Exception ex) {
                player.sendMessage("Error saving map.");
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
        if (args[0].equalsIgnoreCase("join")) {
            List<String> result = new ArrayList<>();
            for (Player player : Bukkit.getServer().getOnlinePlayers())
                result.add(player.getName());
            return result;
        }
        if (args[0].equalsIgnoreCase("create")) return Brickball.getInstance().getTemplateManager().listTemplateIDs();
        if (args[0].equalsIgnoreCase("map")) return List.of("save", "list");
        if (args[0].equalsIgnoreCase("jointeam")) return List.of("1", "2", "3");
        if (args[0].equalsIgnoreCase("teamColor")) {
            if (args.length == 2) return List.of("1", "2");
            return List.of("black", "blue", "cyan", "gray", "green", "lightblue","lightgray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");
        }
        if (args[0].equalsIgnoreCase("setting") && args.length == 2) {
            List<String> result = new ArrayList<>();
            for (NamespacedKey key : MatchSettings.Setting.keys)
                result.add(key.getKey());
            return result;
        }
        if (args.length == 1) return List.of("create", "join", "jointeam", "leave", "map", "start", "teamcolor", "setting", "setworld");
        return null;
    }

    private boolean setWorldCommand(Player player, String[] args) {
        if (!player.hasPermission("brickball.world.set"))
            return insufficientPermissions(player);
        if (args.length < 2) {
            Brickball.getInstance().setMatchWorld(player.getWorld());
            if (player.getWorld().getName().equalsIgnoreCase("world"))
                player.sendMessage("WARNING: You have the default world as the match world. This is usually a bad idea, as this world may be destroyed. You should probably run this command in another world.");
            else
                player.sendMessage("WARNING: You have set your current world as the match world. Don't do this if you care about anything in this world.");
            player.sendMessage("Brickball match world successfully set.");
        } else {
            World newWorld = Bukkit.getServer().getWorld(args[1]);
            if (newWorld != null) {
                Brickball.getInstance().setMatchWorld(newWorld);
                player.sendMessage("Brickball match world successfully set.");
            }
        }
        return true;
    }

    private boolean insufficientPermissions(Player player) {
        player.sendMessage("Insufficient Permissions");
        return true;
    }
}

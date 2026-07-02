package com.github.drfiveminusmint.brickball.command;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.arena.TemplateManager;
import com.github.drfiveminusmint.brickball.lobby.BrickballFormat;
import com.github.drfiveminusmint.brickball.lobby.Lobby;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.brickball.match.MatchState;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage("Usage: /brickball (join/jointeam/create/leave/map/start/pause/unpause)");
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
        if (args[0].equalsIgnoreCase("setmap")) return setMapCommand(player, args);
        if (args[0].equalsIgnoreCase("setworld")) return setWorldCommand(player, args);
        if (args[0].equalsIgnoreCase("admin")) return adminCommand(player, args);
        if (args[0].equalsIgnoreCase("pause")) return pauseCommand(player, args);
        if (args[0].equalsIgnoreCase("unpause")) return unpauseCommand(player, args);
        if (args[0].equalsIgnoreCase("ready") || args[0].equalsIgnoreCase("unready")) return readyCommand(player, args);
        if (args[0].equalsIgnoreCase("invite")) return inviteCommand(player, args);
        return false;
    }

    private boolean inviteCommand(Player player, String[] args) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Please supply a player to invite.", NamedTextColor.RED));
            return true;
        }
        Player other = Bukkit.getPlayer(args[1]);
        if (other == null) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        lobby.invite(other, player);
        return true;
    }

    private boolean readyCommand(Player player, String[] args) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (Brickball.getInstance().getMatchManager().getMatchByPlayer(player) != null) {
            player.sendMessage(Component.text("The match has already started!", NamedTextColor.RED));
            return true;
        }
        if (lobby.toggleReady(player))
            lobby.sendMessage(player.displayName().append(Component.text(" is now ready!", NamedTextColor.GOLD)));
        else
            lobby.sendMessage(player.displayName().append(Component.text(" is no longer ready.", NamedTextColor.GOLD)));
        return true;
    }

    private boolean setMapCommand(Player player, String[] args) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (!player.equals(lobby.getHost())) {
            player.sendMessage(Component.text("You can only change the map if you're the lobby host.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball setmap (mapName)");
            return true;
        }
        ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(args[1]);
        if (template == null) {
            Component message = Component.text(args[1], NamedTextColor.DARK_RED)
                    .append(Component.text(" is not a Brickball map. Legal maps for this format are: ", NamedTextColor.RED));
            for (ArenaTemplate candidate : lobby.getFormat().getValidMaps()) {
                message = message.append(Component.text(candidate.getID(), NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball setmap " + candidate.getID())))
                        .append(Component.text(" "));
            }
            player.sendMessage(message);
            return true;
        }
        if (!lobby.setMap(template, player)) {
            Component message = Component.text(args[1], NamedTextColor.DARK_RED)
                    .append(Component.text(" is not legal in this Brickball format. Legal maps for this format are: ", NamedTextColor.RED));
            for (ArenaTemplate candidate : lobby.getFormat().getValidMaps()) {
                message = message.append(Component.text(candidate.getID(), NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball setmap " + candidate.getID())))
                        .append(Component.text(" "));
            }
            player.sendMessage(message);
        }
        lobby.sendMessage(Component.text("[Brickball] ", NamedTextColor.GOLD)
                .append(player.displayName())
                .append(Component.text(" has selected ", NamedTextColor.GOLD))
                .append(Component.text(template.getID(), NamedTextColor.AQUA))
                .append(Component.text(" as the next map.", NamedTextColor.GOLD)));
        return true;
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

    // Sets the team color of one of the lobby's teams
    public boolean teamColorCommand (Player player, String[] args) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (!player.equals(lobby.getHost())) {
            player.sendMessage(Component.text("You can only change the team colors if you're the lobby host.", NamedTextColor.RED));
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
        // setting the lobby's team colors also propagates these changes down
        lobby.setTeamColor(color, team-1);
        return true;
    }

    // Creates a lobby with the format specified in args[1] and optional flags in args[2]
    public boolean createCommand(Player player, String[] args) {
        if (!player.hasPermission("brickball.create")) return insufficientPermissions(player);
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball create (format)");
            return true;
        }
        if (Brickball.getInstance().getLobbyList().getLobbyByPlayer(player) != null) {
            player.sendMessage("You're already in a Brickball lobby.");
            return true;
        }
        if (Brickball.getInstance().getMatchWorld() == null)
        {
            player.sendMessage("Error: Match world not set. Set it with /brickball setWorld");
            return true;
        }
        /*
        ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(args[1]);
        if (template == null) {
            player.sendMessage(String.format("Couldn't find arena template: %s", args[1]));
            return true;
        }
        */
        BrickballFormat format = null;
        for (BrickballFormat candidate : Brickball.getInstance().getFormats())
            if (candidate.getName().equalsIgnoreCase(args[1])) {
                format = candidate;
                break;
            }
        if (format == null) {
            Component message = Component.text(args[1], NamedTextColor.DARK_RED)
                    .append(Component.text(" is not an available Brickball format. Available formats are: ", NamedTextColor.RED));
            for (BrickballFormat candidate : Brickball.getInstance().getFormats()) {
                message = message.append(Component.text(candidate.getName(), NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball create " + candidate.getName())))
                        .append(Component.text(" "));
            }
            player.sendMessage(message);
            return true;
        }
        String flags = "";
        if (args.length > 2 && args[2].charAt(0) == '-')
            flags = args[2];
        Lobby newLobby = new Lobby(format, flags.contains("p"));
        Bukkit.getServer().broadcast(Component.text("[Brickball] ", NamedTextColor.GOLD)
                .append(player.displayName())
                .append(Component.text(" has created a ", NamedTextColor.GOLD))
                .append(Component.text(format.getName(), NamedTextColor.YELLOW)).append(Component.text(" lobby. ", NamedTextColor.GOLD))
                .append(Component.text("Click to join!", NamedTextColor.AQUA)).clickEvent(ClickEvent.runCommand("/brickball join " + player.getName())));
        newLobby.join(player, 2); // start as spectator
        newLobby.setHost(player);
        player.sendMessage(Component.text("[Start Match]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball start")));
        return true;
    }

    // Joins the current lobby of the player specified in args[1]
    public boolean joinCommand(Player player, String[] args) {
        if (!player.hasPermission("brickball.join")) return insufficientPermissions(player);
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball join (user)");
            return true;
        }
        if (Brickball.getInstance().getLobbyList().getLobbyByPlayer(player) != null) {
            player.sendMessage(Component.text("You're already in a Brickball lobby!", NamedTextColor.RED));
            return true;
        }
        Player otherPlayer = Bukkit.getPlayer(args[1]);
        if (otherPlayer == null) {
            player.sendMessage(Component.text(String.format("Player %s not found.", args[1]), NamedTextColor.RED));
            return true;
        }
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(otherPlayer);
        if (lobby == null) {
            player.sendMessage(Component.text(String.format("Player %s has no active Brickball lobby you can join.", args[1]), NamedTextColor.RED));
            return true;
        }
        if(!lobby.join(player, 0)) {
            player.sendMessage(Component.text(String.format("You don't have permission to join this match.", args[1]), NamedTextColor.RED));
            return true;
        }
        return true;
    }

    // Leaves the player's current lobby
    public boolean leaveCommand(Player player) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby != null) {
            lobby.leave(player);
            player.sendMessage("You have left the lobby.");
            return true;
        }
        player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
        return true;
    }

    // Changes the player's team to the one specified in args[1]
    // Fails if a match is currently running, as otherwise their lobby and game teams could become desynchronized.
    public boolean teamCommand(Player player, String[] args) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (lobby.getCurrentMatchState() != null && !lobby.getCurrentMatchState().equals(MatchState.PAUSED)) {
            player.sendMessage("You can't switch teams while a match is running. Use '/brickball pause' or wait for the end of the match.");
        }
        if (args.length < 2) {
            player.sendMessage("Specify a team to join (Spectator is team 3).");
            return true;
        }
        try {
            if (!lobby.joinTeam(player, Integer.parseInt(args[1]) - 1)) {
                player.sendMessage(Component.text("Error joining team.", NamedTextColor.RED));
                return true;
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("Error joining team.", NamedTextColor.RED));
            return true;
        }
        return true;
    }

    // Alters match settings in the player's current lobby.
    // These changes propagate downwards to the active match if one is present.
    public boolean settingCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball setting (setting) <value>");
            return true;
        }
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (lobby.getHost() != player) {
            player.sendMessage(Component.text("Only the host can change match settings.", NamedTextColor.RED));
            return true;
        }
        NamespacedKey key = MatchSettings.Setting.getKey(args[1]);
        if (key == null) {
            player.sendMessage(Component.text("Unknown match setting: " + args[1], NamedTextColor.RED));
            return true;
        }
        Object value = lobby.getMatchSetting(key);
        if (args.length == 2) {
            player.sendMessage(String.format("Match setting %s is set to: %s", key.getKey(), value));
            return true;
        }
        switch (value) {
            case Integer integer -> {
                try {
                    int i = Integer.parseInt(args[2]);
                    lobby.setMatchSetting(key, i);
                } catch (NumberFormatException exception) {
                    player.sendMessage(Component.text("Could not parse argument: " + args[2], NamedTextColor.RED));
                    return true;
                }
            }
            case Double v -> {
                try {
                    double d = Double.parseDouble(args[2]);
                    lobby.setMatchSetting(key, d);
                } catch (NumberFormatException exception) {
                    player.sendMessage(Component.text("Could not parse argument: " + args[2], NamedTextColor.RED));
                    return true;
                }
            }
            case Boolean aBoolean -> {
                try {
                    boolean b = Boolean.parseBoolean(args[2]);
                    lobby.setMatchSetting(key, b);
                } catch (NumberFormatException exception) {
                    player.sendMessage(Component.text("Could not parse argument: " + args[2], NamedTextColor.RED));
                    return true;
                }
            }
            case null, default -> {
                player.sendMessage(Component.text("Couldn't set match setting. This usually indicates a bug in the plugin. Contact the developer and show them this error:", NamedTextColor.DARK_RED));
                player.sendMessage(Component.text("Couldn't edit match setting of type " + value.getClass().getName(), NamedTextColor.DARK_RED));
                return true;
            }
        }
        player.sendMessage("Match setting has been set!");
        return true;
    }

    public boolean mapCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Usage: /brickball map (save/delete/list)");
            return true;
        }
        if (args[1].equalsIgnoreCase("list")) {
            if (!player.hasPermission("brickball.map.list")) return insufficientPermissions(player);
            for (ArenaTemplate template : Brickball.getInstance().getTemplateManager().templates.values())
                player.sendMessage(Component.text(template.getID()));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage("Usage: /brickball map save/delete (id)");
            return true;
        }
        if (args[1].equalsIgnoreCase("save")) {
            if (!player.hasPermission("brickball.map.save")) return insufficientPermissions(player);
            TemplateManager manager = Brickball.getInstance().getTemplateManager();
            ArenaTemplate newTemplate;
            try {
                newTemplate = ArenaTemplate.createByID(args[2], player.getLocation(), new BukkitWorld(player.getWorld()), true);
            } catch (Exception ex) {
                player.sendMessage(Component.text("Error saving map.", NamedTextColor.RED));
                return true;
            }
            // flush all running instances of this map
            Brickball.getInstance().getMatchManager().flushMatches(args[2]);
            // Template Manager handles creation logic
            manager.registerTemplate(newTemplate);
            player.sendMessage("Map creation successful!");
            return true;
        } else if (args[1].equalsIgnoreCase("delete")) {
            if (!player.hasPermission("brickball.map.delete")) return insufficientPermissions(player);
            ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(args[2]);
            if (template == null) {
                player.sendMessage(String.format("Couldn't find arena template: %s", args[2]));
                return true;
            }
            BrickballMatch match;
            // flush all running instances of this map
            Brickball.getInstance().getMatchManager().flushMatches(args[2]);
            // Template manager handles deletion logic
            if (Brickball.getInstance().getTemplateManager().deleteTemplate(template))
                player.sendMessage(String.format("Deleting arena template: %s", args[2]));
            else
                player.sendMessage(Component.text("Error deleting map.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage("Usage: /brickball map (save/delete/list)");
        return true;
    }

    public boolean startCommand(Player player) {
        Lobby lobby = Brickball.getInstance().getLobbyList().getLobbyByPlayer(player);
        if (lobby == null) {
            player.sendMessage(Component.text("You're not in a Brickball lobby.", NamedTextColor.RED));
            return true;
        }
        if (player != lobby.getHost()) {
            player.sendMessage(Component.text("Only a lobby host can use this command.", NamedTextColor.RED));
            return true;
        }
        if (lobby.tryStartMatch(true)) {
            player.sendMessage("Match starting! Please wait...");
        } else {
            player.sendMessage(Component.text("Error starting match.", NamedTextColor.RED));
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
        if (args[0].equalsIgnoreCase("create")) {
            List<String> result = new ArrayList<>();
            for (BrickballFormat format : Brickball.getInstance().getFormats())
                result.add(format.getName());
            return result;
        }
        if (args[0].equalsIgnoreCase("map")) {
            if (args.length == 2)
                return List.of("save", "delete", "list");
            if (args.length > 2 && args[1].equalsIgnoreCase("delete"))
                return Brickball.getInstance().getTemplateManager().listTemplateIDs();
        }
        if (args[0].equalsIgnoreCase("jointeam")) return List.of("1", "2", "3");
        if (args[0].equalsIgnoreCase("teamColor")) {
            if (args.length == 2) return List.of("1", "2");
            return List.of("black", "blue", "cyan", "gray", "green", "lightblue","lightgray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");
        }
        if (args[0].equalsIgnoreCase("setmap")) return Brickball.getInstance().getTemplateManager().listTemplateIDs();
        if (args[0].equalsIgnoreCase("setting") && args.length == 2) {
            List<String> result = new ArrayList<>();
            for (NamespacedKey key : MatchSettings.Setting.keys)
                result.add(key.getKey());
            return result;
        }
        if (args.length == 1) return List.of("create", "join", "jointeam", "leave", "map", "start", "teamcolor", "setting", "setworld", "admin", "pause", "unpause");
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

    public boolean pauseCommand(Player player, String[] args) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage(Component.text("You're not in an active Brickball match.", NamedTextColor.RED));
            return true;
        }
        if (!match.getIsHost(player)) {
            player.sendMessage(Component.text("Only the host can pause the match.", NamedTextColor.RED));
            return true;
        }
        if (match.pause(true)) {
            player.sendMessage(Component.text("[Brickball] Match paused. ", NamedTextColor.WHITE)
                    .append(Component.text("Click here to unpause.", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball unpause"))));
        } else {
            player.sendMessage("Could not pause match: The match is not currently running!");
        }
        return true;
    }

    public boolean unpauseCommand(Player player, String[] args) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.sendMessage(Component.text("You're not in an active Brickball match.", NamedTextColor.RED));
            return true;
        }
        if (!match.getIsHost(player)) {
            player.sendMessage(Component.text("Only the host can unpause the match.", NamedTextColor.RED));
            return true;
        }
        if (match.unpause()) {
            player.sendMessage("[Brickball] Match unpaused.");
        } else {
            player.sendMessage("Could not unpause match: The match is not currently paused!");
        }
        return true;
    }

    private boolean insufficientPermissions(Player player) {
        player.sendMessage(Component.text("Insufficient Permissions", NamedTextColor.RED));
        return true;
    }
}

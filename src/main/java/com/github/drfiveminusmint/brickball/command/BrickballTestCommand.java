package com.github.drfiveminusmint.brickball.command;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

public class BrickballTestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You must be a player to use this command");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Usage: /brickballtest (create/test) (id)");
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) return createCommand(player, args[1]);
        if (args[0].equalsIgnoreCase("test")) return testCommand(player, args[1]);
        if (args[0].equalsIgnoreCase("joinMatch")) return joinMatchCommand(player);
        if (args[0].equalsIgnoreCase("joinTeam")) return joinTeamCommand(player, args[1]);
        if (args[0].equalsIgnoreCase("startGame")) return startMatchCommand(player);
        if (args[0].equalsIgnoreCase("cleanup")) return cleanupCommand(player);
        return false;
    }

    public boolean createCommand(CommandSender sender, String id)
    {
        World WEWorld = new BukkitWorld(((Player) sender).getWorld());
        ArenaTemplate newArena = ArenaTemplate.createByID(id, ((Player) sender).getLocation(), WEWorld, true);
        newArena.saveSchematic(WEWorld);
        if (Brickball.getInstance().getTemplateManager().registerTemplate(newArena)) {
            sender.sendMessage("Successfully created template " + id);
            return true;
        }
        sender.sendMessage("Error creating template " + id);

        return false;
    }

    public boolean testCommand(CommandSender sender, String id)
    {
        ArenaTemplate template = Brickball.getInstance().getTemplateManager().findTemplate(id);
        if (template == null) {
            sender.sendMessage("Error: could not find template " + id);
            return true;
        }
        Brickball.getInstance().getMatchManager().startMatch(template, 10);
        sender.sendMessage("Successfully created a new match!");
        return true;
    }

    public boolean cleanupCommand(CommandSender sender) {
        Brickball.getInstance().getMatchManager().stopAllMatches();
        sender.sendMessage("Stopping all matches!");
        ((Player) sender).setGameMode(GameMode.CREATIVE);
        return true;
    }
    public boolean joinMatchCommand(Player player) {
        if (Brickball.getInstance().getMatchManager().auto(player) != null) return true;
        player.sendMessage("Failed to find a match!");
        return true;
    }

    public boolean startMatchCommand(Player player) {
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null)
        {
            player.sendMessage("You must join a match first.");
            return true;
        }
        match.startMatch();
        return true;
    }

    public boolean joinTeamCommand(Player player, String teamName)
    {
        int team;
        try {
            team = Integer.parseInt(teamName);
        } catch (NumberFormatException ex) {
            player.sendMessage("Enter a numerical value of a team to join.");
            return true;
        }
        BrickballMatch match = Brickball.getInstance().getMatchManager().getMatchByPlayer(player);
        if (match == null)
        {
            player.sendMessage("You must join a match first.");
            return true;
        }
        if (match.joinTeam(player, team))
            return true;
        player.sendMessage("Error joining team.");
        return true;
    }

}

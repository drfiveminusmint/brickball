package com.github.drfiveminusmint.brickball.lobby;

import com.github.drfiveminusmint.brickball.Brickball;
import com.github.drfiveminusmint.brickball.arena.ArenaTemplate;
import com.github.drfiveminusmint.brickball.match.BrickballMatch;
import com.github.drfiveminusmint.brickball.match.MatchSettings;
import com.github.drfiveminusmint.brickball.match.MatchState;
import com.github.drfiveminusmint.brickball.util.BrickballColor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;

public class Lobby implements ForwardingAudience {
    private final HashMap<Player, Boolean> readyPlayers = new HashMap<>();
    private final HashSet<Player> invited = new HashSet<>();
    private BrickballFormat format;

    private final MatchSettings savedSettings;
    private @Nullable Player host;
    // Cosmetic scoreboard to display name colors. Has no objectives.
    private final Scoreboard lobbyScoreboard;
    private Objective cosmeticObjective;
    private final Team[] lobbyTeams = new Team[3];
    private final BrickballColor[] teamColors = {BrickballColor.RED, BrickballColor.BLUE, BrickballColor.LIGHT_GRAY, BrickballColor.LIGHT_GRAY};
    private boolean isPrivate;
    private @Nullable BrickballMatch activeMatch;
    private ArenaTemplate nextMap;

    // Constructor
    // Automatically registers itself with the LobbyManager
    public Lobby(BrickballFormat format, boolean isPrivate) {
        savedSettings = MatchSettings.cloneDefault();
        setFormat(format, null);
        // Debug
        if (this.format.getIsRated())
            Brickball.getInstance().getLogger().log(Level.INFO, "Rated Match Created!");
        else
            Brickball.getInstance().getLogger().log(Level.INFO, "Unrated Match Created!");
        this.isPrivate = isPrivate;
        Brickball.getInstance().getLobbyList().registerLobby(this);
        // Setup a cosmetic scoreboard
        lobbyScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        cosmeticObjective = lobbyScoreboard.registerNewObjective("lobbyDummy", Criteria.DUMMY, Component.text("Lobby", NamedTextColor.AQUA));
        cosmeticObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        cosmeticObjective.setAutoUpdateDisplay(true);
        lobbyTeams[0] = lobbyScoreboard.registerNewTeam("Team 1");
        lobbyTeams[1] = lobbyScoreboard.registerNewTeam("Team 2");
        lobbyTeams[2] = lobbyScoreboard.registerNewTeam("Spectators");
        for (int i = 0; i < lobbyTeams.length; i++) {
            lobbyTeams[i].color(teamColors[i].textColor);
            lobbyTeams[i].setAllowFriendlyFire(false);
        }
        cosmeticObjective.getScore("Players").setScore(0);
        cosmeticObjective.getScore("Spectators").setScore(0);
        cosmeticObjective.getScore("Ready").setScore(0);
        // set initial map
        nextMap = (ArenaTemplate) format.getValidMaps().toArray()
                [new Random(System.currentTimeMillis()).nextInt(format.getValidMaps().size())];

    }

    public Lobby(BrickballFormat format) { this(format, false); }

    // Join the lobby, starting as the specified team
    // Returns false if the player cannot join this lobby
    public boolean join (Player player, int startingTeamID) {
        if (isPrivate && !invited.contains(player)) return false;
        // kick them to the spectators if their preferred team is full
        if (lobbyTeams[startingTeamID].getSize() >= format.getMaxPlayersPerTeam() && startingTeamID != lobbyTeams.length-1) {
            startingTeamID = lobbyTeams.length-1;
        }
        // don't join if already present
        if (readyPlayers.containsKey(player)) return false;
        readyPlayers.put(player, false);
        // setup two-way links with the lobby and match manager
        Brickball.getInstance().getLobbyList().addPlayerEntry(this, player);
        if (activeMatch != null)
            Brickball.getInstance().getMatchManager().joinMatch(player, activeMatch);
        else
            player.setScoreboard(lobbyScoreboard);
        sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" joined the lobby.")));
        player.sendMessage(Component.text("Current map: ", NamedTextColor.GOLD).append(Component.text(nextMap.getID(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("[Join Team 1]", teamColors[0].textColor).clickEvent(ClickEvent.runCommand("/brickball jointeam 1")));
        player.sendMessage(Component.text("[Join Team 2]", teamColors[1].textColor).clickEvent(ClickEvent.runCommand("/brickball jointeam 2")));
        player.sendMessage(Component.text("[Ready]", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/brickball ready")));
        return joinTeam(player, startingTeamID);
    }

    // Wrapper method for joining a team
    public boolean joinTeam(Player player, int teamID) {
        if (teamID < 0 || teamID > 2) return false;
        // don't allow them to join a team that's full
        if (lobbyTeams[teamID].getSize() >= format.getMaxPlayersPerTeam() && teamID != lobbyTeams.length - 1)
            return false;
        if (activeMatch != null && !activeMatch.joinTeam(player, teamID)) {
            return false;
        }
        for (Team team : lobbyTeams) team.removePlayer(player);
        lobbyTeams[teamID].addPlayer(player);

        if (teamID < lobbyTeams.length-1)
            sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(String.format(" joined Team %d.", teamID+1))));
        else
            sendMessage(Component.text("[Brickball] ").append(player.displayName()).append(Component.text(" is now spectating")));
        // update the scoreboard and try to start the match
        tryStartMatch(false);
        return true;
    }

    // Invite a player
    // Returns false if the specified player cannot be invited
    public boolean invite(Player otherPlayer, @Nullable Player requester) {
        if (requester != null) {
            if (requester != host) {
                requester.sendMessage(Component.text("You don't have permission to invite players to this lobby.", NamedTextColor.RED));
                return false;
            }
            if (invited.contains(otherPlayer)) {
                requester.sendMessage(Component.text(String.format("%s is already invited.", otherPlayer.displayName()), NamedTextColor.RED));
                return false;
            }
            otherPlayer.sendMessage(requester.displayName()
                    .append(Component.text("%s has invited you to a ", NamedTextColor.GOLD))
                    .append(Component.text(format.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" lobby.", NamedTextColor.GOLD))
                    .append(Component.text(" Click to join!", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/lobby join " + requester.getName()))));
        }
        return invited.add(otherPlayer);
    }

    // Leave the lobby
    public boolean leave (Player player) {
        if (!Brickball.getInstance().getLobbyList().removePlayerEntry(player))
            return false;
        if (readyPlayers.remove(player) == null)
            return false;
        for (Team team : lobbyTeams) team.removePlayer(player);
        if (activeMatch != null)
            Brickball.getInstance().getMatchManager().leaveMatch(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        if (isEmpty())
            shutdown();
        else
            tryStartMatch(false);
        return true;
    }

    // Set the format
    // player = null indicates the server, rather than a player, is making this change
    public boolean setFormat(BrickballFormat newFormat, @Nullable Player player) {
        if (player != null && player != host) return false;
        format = newFormat;
        // Overwrite settings
        for (NamespacedKey key : MatchSettings.Setting.keys) {
            savedSettings.set(key, format.getSettings().get(key));
        }
        return true;
    }

    public boolean setMap(ArenaTemplate template, @Nullable Player player) {
        if (player != null && player != host)  {
            player.sendMessage(Component.text("You are not the host!",NamedTextColor.RED));
            return false;
        }
        if (!format.getValidMaps().contains(template))
            return false;
        nextMap = template;
        return true;
    }

    // Create a new match with the specified arguments
    private boolean createAndStartMatch() {
        if (activeMatch != null) return false;
        if (nextMap == null) return false;
        if (lobbyTeams[0].getSize() < format.getMinPlayersPerTeam() || lobbyTeams[1].getSize() < format.getMinPlayersPerTeam())
            return false;
        activeMatch = Brickball.getInstance().getMatchManager().createMatch(nextMap, 1);
        if (activeMatch == null) return false;
        // Overwrite settings
        activeMatch.overwriteSettings(savedSettings);
        activeMatch.setTeamColor(teamColors[0], 0);
        activeMatch.setTeamColor(teamColors[1], 1);
        for(int i = 0; i < 3; i++) {
            for(String entry : lobbyTeams[i].getEntries()) {
                // These will only ever be players
                activeMatch.joinTeam(Bukkit.getPlayer(entry), i);
            }
        }
        activeMatch.setReturningLobby(this);
        activeMatch.reportConfigDone(); // start as soon as arena is loaded
        return true;
    }

    public void returnPlayersToLobby() {
        for (Player player : readyPlayers.keySet()) {
            player.setScoreboard(lobbyScoreboard);
            readyPlayers.put(player, false);
        }
        // sever match link
        activeMatch.setReturningLobby(null);
        activeMatch = null;
        updateScoreboard();
    }

    public boolean shutdown() {
        for (Player player : readyPlayers.keySet())
            leave(player);
        if (activeMatch != null)
            Brickball.getInstance().getMatchManager().endMatch(activeMatch);
        Brickball.getInstance().getLobbyList().unregisterLobby(this);
        return true;
    }

    // Toggles the player's ready state. Returns the new value.
    public boolean toggleReady(Player player) {
        // failsafe
        if (!readyPlayers.containsKey(player)) return false;
        boolean ready = !readyPlayers.get(player);
        readyPlayers.put(player, ready);
        tryStartMatch(false);
        return ready;
    }

    // Start the match
    public boolean tryStartMatch(boolean forceStart) {
        // start regardless of ready status
        if (forceStart)
            return createAndStartMatch();
        // check both non-spectator teams to make sure everyone's ready
        updateScoreboard();
        if (cosmeticObjective.getScore("Players").getScore() != 0
                && cosmeticObjective.getScore("Players").getScore() == cosmeticObjective.getScore("Ready").getScore())
            return createAndStartMatch();
        return false;
    }

    private void updateScoreboard() {
        int numPlayersReady = 0;
        for (String s : lobbyTeams[0].getEntries()) {
            Player p = Bukkit.getServer().getPlayer(s);
            if (p != null) {
                if (readyPlayers.get(p))
                    numPlayersReady++;
            }
        }
        for (String s : lobbyTeams[1].getEntries()) {
            Player p = Bukkit.getServer().getPlayer(s);
            if (p != null) {
                if (readyPlayers.get(p))
                    numPlayersReady++;
            }
        }
        // update scoreboard
        cosmeticObjective.getScore("Players").setScore(readyPlayers.size() - lobbyTeams[2].getSize());
        cosmeticObjective.getScore("Ready").setScore(numPlayersReady);
        cosmeticObjective.getScore("Spectators").setScore(lobbyTeams[2].getSize());
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return readyPlayers.keySet();
    }

    public boolean isEmpty() {return readyPlayers.isEmpty();}

    // Wrapper method to access match settings
    public Object getMatchSetting(NamespacedKey key) { return  savedSettings.get(key);}

    // Wrapper method to modify match settings
    // Returns false if the match setting is not set
    public boolean setMatchSetting(NamespacedKey key, Object value) {
        if (savedSettings.get(key) == null) return false;
        savedSettings.set(key, value);
        return true;
    }

    public boolean setTeamColor(BrickballColor color, int teamID) {
        if (teamID < 0 || teamID > 2) return false;
        teamColors[teamID] = color;
        lobbyTeams[teamID].color(color.textColor);
        if (activeMatch != null)
            activeMatch.setTeamColor(color, teamID);
        return true;
    }

    public void setHost(@Nullable Player newHost) { host = newHost;}
    public Player getHost() { return host;}
    public BrickballFormat getFormat() { return format; }

    public @Nullable MatchState getCurrentMatchState() {
        if (activeMatch == null)
            return null;
        return activeMatch.getState();
    }
}

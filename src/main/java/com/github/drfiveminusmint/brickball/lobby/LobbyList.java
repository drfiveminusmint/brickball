package com.github.drfiveminusmint.brickball.lobby;

import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class LobbyList implements Iterable<Lobby> {
    private final HashSet<Lobby> lobbies = new HashSet<>();
    private final HashMap<Player, Lobby> playerMap = new HashMap<>();

    // Gets the lobby the specified player is in
    @Nullable
    public Lobby getLobbyByPlayer(Player player) {
        return playerMap.get(player);
    }

    public boolean addPlayerEntry(Lobby lobby, Player player) {
        return (playerMap.put(player, lobby) == null);
    }

    public boolean removePlayerEntry(Player player) {
        Lobby lobby = playerMap.remove(player);
        return  (lobby != null);
    }

    public boolean registerLobby(Lobby lobby) {
        return lobbies.add(lobby);
    }

    public boolean unregisterLobby(Lobby lobby) {
        for (Audience audience : lobby.audiences())
            playerMap.remove((Player) audience);
        return lobbies.remove(lobby);
    }

    public void shutdownAll() {
        for (Lobby lobby : lobbies)
            lobby.shutdown();
    }

    @NotNull
    @Override
    public Iterator<Lobby> iterator() {
        return lobbies.iterator();
    }
}

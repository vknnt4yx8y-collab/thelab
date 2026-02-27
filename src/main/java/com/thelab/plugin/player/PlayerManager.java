package com.thelab.plugin.player;

import com.thelab.plugin.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

/** Manages all LabPlayer instances. */
public class PlayerManager {

    private final Map<UUID, LabPlayer> players = new HashMap<>();

    /** Gets or creates a LabPlayer for the given player. */
    public LabPlayer getLabPlayer(Player player) {
        return players.computeIfAbsent(player.getUniqueId(),
                uuid -> new LabPlayer(uuid, player.getName()));
    }

    /** Gets an existing LabPlayer by UUID. */
    public Optional<LabPlayer> getLabPlayer(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    /** Returns all LabPlayers currently in the given arena. */
    public List<LabPlayer> getPlayersInArena(Arena arena) {
        List<LabPlayer> list = new ArrayList<>();
        for (LabPlayer lp : players.values()) {
            if (arena.equals(lp.getArena())) list.add(lp);
        }
        return list;
    }

    /** Returns all LabPlayers with the given state. */
    public List<LabPlayer> getPlayersInState(PlayerState state) {
        List<LabPlayer> list = new ArrayList<>();
        for (LabPlayer lp : players.values()) {
            if (lp.getState() == state) list.add(lp);
        }
        return list;
    }

    /** Called when a player joins the server. */
    public void handleJoin(Player player) {
        LabPlayer lp = players.computeIfAbsent(player.getUniqueId(),
                uuid -> new LabPlayer(uuid, player.getName()));
        lp.setName(player.getName());
    }

    /**
     * Called when a player quits the server.
     * If in an arena, restores inventory and removes them from the arena.
     */
    public void handleQuit(Player player) {
        LabPlayer lp = players.get(player.getUniqueId());
        if (lp == null) return;
        if (lp.getArena() != null) {
            Arena arena = lp.getArena();
            arena.removePlayer(player);
            arena.removeSpectator(player);
            lp.restoreInventory();
            lp.setArena(null);
            lp.setState(PlayerState.NONE);
        }
    }

    /** Removes a LabPlayer from tracking (cleanup). */
    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    /** Returns all tracked LabPlayers. */
    public Collection<LabPlayer> getAllPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }
}

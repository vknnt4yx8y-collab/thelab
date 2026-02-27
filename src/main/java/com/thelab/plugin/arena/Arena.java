package com.thelab.plugin.arena;

import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/** Represents a single TheLab arena instance. */
public class Arena {

    private final ArenaConfig config;
    private ArenaState state;
    private final List<UUID> players = new ArrayList<>();
    private final List<UUID> spectators = new ArrayList<>();
    private final ArenaRegionManager regionManager;

    public Arena(ArenaConfig config) {
        this.config = config;
        this.state = config.isEnabled() ? ArenaState.WAITING : ArenaState.DISABLED;
        this.regionManager = new ArenaRegionManager(config);
    }

    // ---- State ----

    public ArenaState getState() { return state; }

    public void setState(ArenaState state) { this.state = state; }

    // ---- Players ----

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

    public void addSpectator(Player player) {
        spectators.add(player.getUniqueId());
    }

    public void removeSpectator(Player player) {
        spectators.remove(player.getUniqueId());
    }

    public boolean isPlayer(Player player) {
        return players.contains(player.getUniqueId());
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public boolean isInArena(Player player) {
        return isPlayer(player) || isSpectator(player);
    }

    /** Returns all online players (non-spectators) currently in the arena. */
    public List<Player> getPlayers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) online.add(p);
        }
        return online;
    }

    /** Returns all online spectators. */
    public List<Player> getSpectators() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) online.add(p);
        }
        return online;
    }

    /** Returns all online participants (players + spectators). */
    public List<Player> getAllParticipants() {
        List<Player> all = new ArrayList<>(getPlayers());
        all.addAll(getSpectators());
        return all;
    }

    public List<UUID> getPlayerUUIDs() { return Collections.unmodifiableList(players); }
    public List<UUID> getSpectatorUUIDs() { return Collections.unmodifiableList(spectators); }

    public int getPlayerCount() { return players.size(); }
    public int getSpectatorCount() { return spectators.size(); }

    public boolean isFull() { return players.size() >= config.getMaxPlayers(); }
    public boolean isEmpty() { return players.isEmpty(); }

    public boolean isJoinable() {
        return config.isEnabled()
                && (state == ArenaState.WAITING || state == ArenaState.STARTING)
                && !isFull();
    }

    // ---- Config delegators ----

    public String getId() { return config.getId(); }
    public String getDisplayName() { return config.getDisplayName(); }
    public int getMinPlayers() { return config.getMinPlayers(); }
    public int getMaxPlayers() { return config.getMaxPlayers(); }
    public ArenaConfig getConfig() { return config; }
    public ArenaRegionManager getRegionManager() { return regionManager; }

    public Location getLobbySpawn() { return config.getLobbySpawn(); }
    public Location getSpectatorSpawn() { return config.getSpectatorSpawn(); }

    // ---- Broadcast ----

    public void broadcast(String message) {
        MessageUtil.broadcast(getAllParticipants(), message);
    }

    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        MessageUtil.broadcastTitle(getAllParticipants(), title, subtitle, fadeIn, stay, fadeOut);
    }

    public void broadcastToPlayers(String message) {
        MessageUtil.broadcast(getPlayers(), message);
    }

    // ---- Cleanup ----

    public void clearAll() {
        players.clear();
        spectators.clear();
    }
}

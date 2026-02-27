package com.thelab.plugin.player;

import com.thelab.plugin.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Wraps a player's state within a TheLab game. */
public class LabPlayer {

    private final UUID uuid;
    private String name;
    private Arena arena;
    private PlayerState state;
    private PlayerInventorySnapshot snapshot;

    public LabPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.state = PlayerState.NONE;
    }

    // ---- Getters / Setters ----

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }

    public PlayerState getState() { return state; }
    public void setState(PlayerState state) { this.state = state; }

    public PlayerInventorySnapshot getSnapshot() { return snapshot; }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    public boolean isOnline() { return Bukkit.getPlayer(uuid) != null; }

    // ---- Arena management ----

    /** Saves the player's current inventory state. */
    public void saveInventory(Player player) {
        this.snapshot = PlayerInventorySnapshot.capture(player);
    }

    /** Restores the player's saved inventory state. */
    public void restoreInventory() {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && snapshot != null) {
            snapshot.restore(player);
            snapshot = null;
        }
    }

    /** Called when a player joins an arena lobby. Saves inventory & prepares player. */
    public void joinArena(Arena arena, Player player) {
        this.arena = arena;
        this.state = PlayerState.LOBBY;
        saveInventory(player);

        // Prepare for arena
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setLevel(0);
        player.setExp(0.0f);
        for (var effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    /** Called when a player leaves the arena. Restores inventory. */
    public void leaveArena() {
        restoreInventory();
        this.arena = null;
        this.state = PlayerState.NONE;
    }
}

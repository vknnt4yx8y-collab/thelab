package com.thelab.plugin.experiment.impl;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.Experiment;
import com.thelab.plugin.experiment.ExperimentConfig;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.game.ScoreManager;
import com.thelab.plugin.utils.MessageUtil;
import com.thelab.plugin.utils.SoundUtil;
import org.bukkit.*;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Boat Wars experiment.
 * Players in boats shoot each other. 5 hits destroys a boat.
 */
public class BoatWarsExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, Integer> boatHealth = new HashMap<>(); // player UUID -> boat HP
    private final Map<UUID, Boat> playerBoats = new HashMap<>();
    private Runnable onEnd;

    public BoatWarsExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.BOAT_WARS; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        int maxHp = config.getInt("boat-health", 5);
        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.BOAT_WARS);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location loc = spawns.isEmpty() ? p.getLocation() : spawns.get(i % spawns.size());
            p.teleport(loc);
            p.getInventory().clear();
            p.getInventory().setItem(0, new ItemStack(Material.BOW));
            p.getInventory().setItem(9, new ItemStack(Material.ARROW, 64));
            p.setGameMode(GameMode.SURVIVAL);

            // Spawn boat and seat player
            if (loc.getWorld() != null) {
                Boat boat = loc.getWorld().spawn(loc, Boat.class);
                boat.addPassenger(p);
                playerBoats.put(p.getUniqueId(), boat);
                boatHealth.put(p.getUniqueId(), maxHp);
            }
            MessageUtil.sendRaw(p, "&eBoat Wars! Shoot enemy boats! HP: " + maxHp);
        }

        // Timer
        BukkitTask timer = new BukkitRunnable() {
            int remaining = config.getDuration();
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                if (remaining <= 0) { cancel(); end(); onEnd.run(); return; }
                if (arena.getPlayers().size() <= 1) { cancel(); end(); onEnd.run(); return; }
                remaining--;
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 20L, 20L);
        tasks.add(timer);
    }

    /** Called when a player's boat is hit by an arrow. */
    public void handleBoatHit(Player victim) {
        if (!running || !arena.isPlayer(victim)) return;
        int hp = boatHealth.getOrDefault(victim.getUniqueId(), 0);
        hp--;
        if (hp <= 0) {
            // Boat destroyed
            Boat boat = playerBoats.remove(victim.getUniqueId());
            if (boat != null) boat.remove();
            boatHealth.remove(victim.getUniqueId());
            MessageUtil.sendRaw(victim, "&cYour boat was destroyed!");
            arena.broadcastToPlayers("&e" + victim.getName() + "'s boat was destroyed!");
            SoundUtil.playElimination(victim);
            boolean shouldEnd = eliminate(victim);
            if (shouldEnd) { end(); if (onEnd != null) onEnd.run(); }
        } else {
            boatHealth.put(victim.getUniqueId(), hp);
            MessageUtil.sendRaw(victim, "&cBoat HP: &e" + hp);
        }
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        for (Boat b : playerBoats.values()) {
            if (b.isValid()) b.remove();
        }
        playerBoats.clear();
        boatHealth.clear();

        List<Player> survivors = arena.getPlayers();
        if (!survivors.isEmpty()) {
            scoreManager.addScore(survivors.get(0).getUniqueId(), 3);
        }
    }
}

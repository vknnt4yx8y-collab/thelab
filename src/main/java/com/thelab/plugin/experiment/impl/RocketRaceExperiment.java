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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Rocket Race experiment.
 * Players fly through checkpoints using Elytra + Firework Rockets.
 * First to complete all checkpoints wins.
 */
public class RocketRaceExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, Integer> playerCheckpoints = new HashMap<>();
    private List<Location> checkpoints;
    private Runnable onEnd;

    public RocketRaceExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.ROCKET_RACE; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        checkpoints = arena.getConfig().getSpawnsFor(ExperimentType.ROCKET_RACE);
        List<Location> spawns = checkpoints.isEmpty()
                ? (players.isEmpty() ? new ArrayList<>() : List.of(players.get(0).getLocation()))
                : List.of(checkpoints.get(0));

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(0));
            giveKit(p);
            playerCheckpoints.put(p.getUniqueId(), 0);
            MessageUtil.sendRaw(p, "&eRocket Race! Fly through &a" + checkpoints.size() + "&e checkpoints!");
        }

        // Checkpoint detection
        BukkitTask checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                checkCheckpoints();
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 5L, 5L);
        tasks.add(checkTask);

        // Replenish rockets
        BukkitTask rocketTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                for (Player p : arena.getPlayers()) {
                    ItemStack rockets = p.getInventory().getItem(1);
                    if (rockets == null || rockets.getType() != Material.FIREWORK_ROCKET || rockets.getAmount() < 8) {
                        p.getInventory().setItem(1, new ItemStack(Material.FIREWORK_ROCKET, 16));
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 40L, 40L);
        tasks.add(rocketTask);

        // Timer
        BukkitTask timer = new BukkitRunnable() {
            int remaining = config.getDuration();
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                if (remaining <= 0) { cancel(); end(); onEnd.run(); return; }
                remaining--;
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 20L, 20L);
        tasks.add(timer);
    }

    private void giveKit(Player p) {
        p.getInventory().clear();
        p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        p.getInventory().setItem(1, new ItemStack(Material.FIREWORK_ROCKET, 16));
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(false);
    }

    private void checkCheckpoints() {
        if (checkpoints.isEmpty()) return;
        for (Player p : arena.getPlayers()) {
            int next = playerCheckpoints.getOrDefault(p.getUniqueId(), 0);
            if (next >= checkpoints.size()) continue;
            Location cp = checkpoints.get(next);
            if (p.getLocation().distance(cp) < 5.0) {
                playerCheckpoints.put(p.getUniqueId(), next + 1);
                int progress = next + 1;
                MessageUtil.sendRaw(p, "&aCheckpoint " + progress + "/" + checkpoints.size() + "!");
                SoundUtil.playScore(p);
                if (progress >= checkpoints.size()) {
                    // Finished!
                    scoreManager.addScore(p.getUniqueId(), 10);
                    arena.broadcastToPlayers("&6" + p.getName() + " &6finished the race!");
                    end();
                    if (onEnd != null) onEnd.run();
                    return;
                }
            }
        }
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        // Score players by checkpoint progress
        playerCheckpoints.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .forEach(e -> scoreManager.addScore(e.getKey(), e.getValue()));
    }
}

package com.thelab.plugin.experiment.impl;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.Experiment;
import com.thelab.plugin.experiment.ExperimentConfig;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.game.ScoreManager;
import com.thelab.plugin.utils.MessageUtil;
import com.thelab.plugin.utils.SoundUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Dodge Ball experiment.
 * Players throw snowballs at each other. Hit = eliminated. Last standing wins.
 */
public class DodgeBallExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private Runnable onEnd;

    public DodgeBallExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.DODGE_BALL; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.DODGE_BALL);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) {
                p.teleport(spawns.get(i % spawns.size()));
            }
            // Give infinite snowballs
            p.getInventory().clear();
            p.getInventory().setItem(0,
                    new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOWBALL, 64));
            p.setGameMode(GameMode.SURVIVAL);
            MessageUtil.sendRaw(p, "&eDodge Ball! Eliminate others with snowballs!");
        }

        // Replenish snowballs every 5s
        BukkitTask replenish = new BukkitRunnable() {
            @Override public void run() {
                if (!running) { cancel(); return; }
                for (Player p : arena.getPlayers()) {
                    org.bukkit.inventory.ItemStack balls = p.getInventory().getItem(0);
                    if (balls == null || balls.getType() != org.bukkit.Material.SNOWBALL || balls.getAmount() < 16) {
                        p.getInventory().setItem(0,
                                new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOWBALL, 64));
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 100L, 100L);
        tasks.add(replenish);

        // Time limit
        BukkitTask timer = new BukkitRunnable() {
            int remaining = config.getDuration();
            @Override public void run() {
                if (!running) { cancel(); return; }
                if (remaining <= 0) {
                    cancel();
                    end();
                    onEnd.run();
                    return;
                }
                if (arena.getPlayers().size() <= 1) {
                    cancel();
                    end();
                    onEnd.run();
                    return;
                }
                remaining--;
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 20L, 20L);
        tasks.add(timer);
    }

    /** Called by DodgeBallListener when a player is hit. */
    public void handleHit(Player victim) {
        if (!running || !arena.isPlayer(victim)) return;
        SoundUtil.playElimination(victim);
        MessageUtil.sendRaw(victim, "&cYou were eliminated!");
        arena.broadcastToPlayers("&e" + victim.getName() + " &cwas eliminated!");
        boolean shouldEnd = eliminate(victim);
        if (shouldEnd) {
            end();
            if (onEnd != null) onEnd.run();
        }
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();

        // Award score based on survival order (already handled by elimination order)
        List<Player> survivors = arena.getPlayers();
        if (!survivors.isEmpty()) {
            scoreManager.addScore(survivors.get(0).getUniqueId(), 3);
        }
    }
}

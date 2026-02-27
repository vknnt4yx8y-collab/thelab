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
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Balloon Pop experiment.
 * Shoot balloons (colored chickens) for points.
 */
public class BalloonPopExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, Integer> balloonPoints = new HashMap<>(); // entity UUID -> point value
    private Runnable onEnd;

    public BalloonPopExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.BALLOON_POP; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.BALLOON_POP);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            p.getInventory().clear();
            p.getInventory().setItem(0, new ItemStack(Material.BOW));
            p.getInventory().setItem(9, new ItemStack(Material.ARROW, 64));
            p.setGameMode(GameMode.ADVENTURE);
            MessageUtil.sendRaw(p, "&eBalloon Pop! Shoot balloons for points!");
        }

        Location spawnBase = spawns.isEmpty()
                ? (players.isEmpty() ? null : players.get(0).getLocation())
                : spawns.get(0);

        if (spawnBase != null) {
            Location arenaMax = arena.getConfig().getArenaMax();
            if (arenaMax == null) arenaMax = spawnBase;
            final Location max = arenaMax;
            final Location base = spawnBase;

            // Spawn balloons periodically
            BukkitTask spawnTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!running) { cancel(); return; }
                    spawnBalloon(base, max);
                }
            }.runTaskTimer(TheLabPlugin.getInstance(), 10L, 30L);
            tasks.add(spawnTask);
        }

        // Replenish arrows
        BukkitTask arrowTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                for (Player p : arena.getPlayers()) {
                    ItemStack arrows = p.getInventory().getItem(9);
                    if (arrows == null || arrows.getType() != Material.ARROW || arrows.getAmount() < 16) {
                        p.getInventory().setItem(9, new ItemStack(Material.ARROW, 64));
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 40L, 40L);
        tasks.add(arrowTask);

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

    private void spawnBalloon(Location base, Location max) {
        if (base.getWorld() == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location loc = com.thelab.plugin.utils.LocationUtil.randomInRegion(base, max);
        loc.setY(loc.getY() + r.nextDouble(2, 8));

        // Random point value
        double roll = r.nextDouble();
        int points;
        if (roll < 0.05) points = config.getInt("balloon-gold-points", 5);       // 5% gold
        else if (roll < 0.2) points = config.getInt("balloon-red-points", 3);    // 15% red
        else if (roll < 0.45) points = config.getInt("balloon-yellow-points", 2); // 25% yellow
        else points = config.getInt("balloon-white-points", 1);                    // 55% white

        // Use a Chicken as balloon
        Chicken balloon = loc.getWorld().spawn(loc, Chicken.class, c -> {
            c.setAI(false);
            c.setGravity(false);
            c.setPersistent(true);
            c.setCustomNameVisible(true);
            c.customName(net.kyori.adventure.text.Component.text(
                    points + " pt" + (points > 1 ? "s" : ""),
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        });
        balloonPoints.put(balloon.getUniqueId(), points);

        // Auto-despawn after 10s
        new BukkitRunnable() {
            @Override
            public void run() {
                if (balloon.isValid()) {
                    balloon.remove();
                    balloonPoints.remove(balloon.getUniqueId());
                }
            }
        }.runTaskLater(TheLabPlugin.getInstance(), 200L);
    }

    /** Called when a player shoots a balloon entity. */
    public void handleBalloonHit(Player shooter, Entity entity) {
        if (!running || !arena.isPlayer(shooter)) return;
        Integer points = balloonPoints.remove(entity.getUniqueId());
        if (points == null) return;
        entity.remove();
        scoreManager.addScore(shooter.getUniqueId(), points);
        SoundUtil.playScore(shooter);
        MessageUtil.sendRaw(shooter, "&a+" + points + " points!");
        // Particle effect
        entity.getLocation().getWorld().spawnParticle(
                org.bukkit.Particle.EXPLOSION, entity.getLocation(), 5, 0.3, 0.3, 0.3, 0.1);
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        // Remove all balloons
        for (UUID uid : balloonPoints.keySet()) {
            Entity e = Bukkit.getEntity(uid);
            if (e != null) e.remove();
        }
        balloonPoints.clear();
    }
}

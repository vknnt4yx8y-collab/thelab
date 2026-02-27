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
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gold Rush experiment.
 * Gold spawns randomly in the arena; players collect it by walking over it.
 */
public class GoldRushExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<Item> spawnedItems = new ArrayList<>();
    private Runnable onEnd;

    public GoldRushExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.GOLD_RUSH; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.GOLD_RUSH);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            MessageUtil.sendRaw(p, "&eGold Rush! Collect gold for points! No PvP!");
        }

        Location arenaMin = arena.getConfig().getArenaMin();
        Location arenaMax = arena.getConfig().getArenaMax();
        if (arenaMin == null || arenaMax == null) arenaMin = spawns.isEmpty() ? null : spawns.get(0);

        final Location spawnBase = arenaMin != null ? arenaMin : (players.isEmpty() ? null : players.get(0).getLocation());

        // Spawn gold items periodically
        if (spawnBase != null) {
            Location arenaMaxFinal = arenaMax != null ? arenaMax : spawnBase;
            Location spawnBaseFinal = spawnBase;
            int intervalMin = config.getInt("spawn-interval-min", 2) * 20;
            int intervalMax = config.getInt("spawn-interval-max", 5) * 20;
            BukkitTask spawnTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!running) { cancel(); return; }
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    Location loc = com.thelab.plugin.utils.LocationUtil.randomInRegion(spawnBaseFinal, arenaMaxFinal);
                    loc.setY(loc.getY() + 1);
                    if (loc.getWorld() == null) return;
                    Material mat = r.nextBoolean() ? Material.GOLD_INGOT : Material.GOLD_BLOCK;
                    Item item = loc.getWorld().dropItem(loc, new ItemStack(mat, 1));
                    item.setPickupDelay(0);
                    spawnedItems.add(item);
                }
            }.runTaskTimer(TheLabPlugin.getInstance(), 20L, intervalMin);
            tasks.add(spawnTask);
        }

        // Collection check
        BukkitTask collectTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                for (Player p : arena.getPlayers()) {
                    // Check if player picked up gold (via inventory)
                    int ingots = p.getInventory().all(Material.GOLD_INGOT).values().stream()
                            .mapToInt(ItemStack::getAmount).sum();
                    int blocks = p.getInventory().all(Material.GOLD_BLOCK).values().stream()
                            .mapToInt(ItemStack::getAmount).sum();
                    if (ingots > 0) {
                        scoreManager.addScore(p.getUniqueId(), ingots * config.getInt("gold-ingot-points", 1));
                        p.getInventory().remove(Material.GOLD_INGOT);
                        SoundUtil.playScore(p);
                    }
                    if (blocks > 0) {
                        scoreManager.addScore(p.getUniqueId(), blocks * config.getInt("gold-block-points", 5));
                        p.getInventory().remove(Material.GOLD_BLOCK);
                        SoundUtil.playScore(p);
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 5L, 5L);
        tasks.add(collectTask);

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

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        spawnedItems.forEach(Item::remove);
        spawnedItems.clear();
    }
}

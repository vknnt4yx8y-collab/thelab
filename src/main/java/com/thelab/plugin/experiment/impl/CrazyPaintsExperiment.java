package com.thelab.plugin.experiment.impl;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.Experiment;
import com.thelab.plugin.experiment.ExperimentConfig;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.game.ScoreManager;
import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Crazy Paints experiment.
 * Players paint floor tiles with their assigned color. Most tiles win.
 */
public class CrazyPaintsExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, DyeColor> playerColors = new HashMap<>();
    private Runnable onEnd;

    private static final DyeColor[] COLOR_POOL = {
            DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.YELLOW,
            DyeColor.PURPLE, DyeColor.ORANGE, DyeColor.CYAN, DyeColor.MAGENTA
    };

    public CrazyPaintsExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.CRAZY_PAINTS; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;
        playerColors.clear();

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.CRAZY_PAINTS);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            DyeColor color = COLOR_POOL[i % COLOR_POOL.length];
            playerColors.put(p.getUniqueId(), color);

            p.getInventory().clear();
            // Iron shovel for painting
            p.getInventory().setItem(0, new ItemStack(Material.IRON_SHOVEL));
            // Splash potions for 3x3 area painting
            p.getInventory().setItem(1, new ItemStack(Material.SPLASH_POTION, 1));
            p.setGameMode(GameMode.ADVENTURE);
            MessageUtil.sendRaw(p, "&eCrazy Paints! Your color: &f" + color.name().replace("_", " "));
        }

        // Paint blocks under players as they walk
        BukkitTask paintTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                for (Player p : arena.getPlayers()) {
                    DyeColor color = playerColors.get(p.getUniqueId());
                    if (color == null) continue;
                    Block below = p.getLocation().subtract(0, 1, 0).getBlock();
                    if (isWoolOrConcrete(below)) {
                        below.setType(Material.valueOf(color.name() + "_WOOL"), false);
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 2L, 2L);
        tasks.add(paintTask);

        // Refill splash potions
        int refillInterval = config.getInt("splash-refill-interval", 15) * 20;
        BukkitTask refillTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                for (Player p : arena.getPlayers()) {
                    p.getInventory().setItem(1, new ItemStack(Material.SPLASH_POTION, 1));
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), refillInterval, refillInterval);
        tasks.add(refillTask);

        // Score update task
        BukkitTask scoreTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                // Count blocks per player color
                countAndUpdateScores();
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 40L, 40L);
        tasks.add(scoreTask);

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

    private void countAndUpdateScores() {
        Location min = arena.getConfig().getArenaMin();
        Location max = arena.getConfig().getArenaMax();
        if (min == null || max == null || min.getWorld() == null) return;

        Map<DyeColor, Integer> colorCounts = new HashMap<>();
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = min.getWorld().getBlockAt(x, y, z);
                    DyeColor dc = woolToDye(b.getType());
                    if (dc != null) colorCounts.merge(dc, 1, Integer::sum);
                }
            }
        }

        // Reset experiment scores and set to block counts
        scoreManager.resetExperimentScores();
        for (Map.Entry<UUID, DyeColor> entry : playerColors.entrySet()) {
            int count = colorCounts.getOrDefault(entry.getValue(), 0);
            scoreManager.addScore(entry.getKey(), count);
        }
    }

    private boolean isWoolOrConcrete(Block block) {
        String name = block.getType().name();
        return name.endsWith("_WOOL") || name.endsWith("_CONCRETE");
    }

    private DyeColor woolToDye(Material mat) {
        String name = mat.name();
        if (name.endsWith("_WOOL")) {
            try { return DyeColor.valueOf(name.replace("_WOOL", "")); }
            catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public void end() {
        running = false;
        countAndUpdateScores();
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
    }
}

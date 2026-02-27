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
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Breaking Blocks experiment.
 * Mine blocks for points. Blocks regenerate.
 */
public class BreakingBlocksExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private Runnable onEnd;

    // Block material -> point value
    private static final Map<Material, Integer> BLOCK_POINTS = new LinkedHashMap<>();
    static {
        BLOCK_POINTS.put(Material.STONE, 1);
        BLOCK_POINTS.put(Material.COAL_ORE, 2);
        BLOCK_POINTS.put(Material.IRON_ORE, 3);
        BLOCK_POINTS.put(Material.GOLD_ORE, 5);
        BLOCK_POINTS.put(Material.DIAMOND_ORE, 10);
        BLOCK_POINTS.put(Material.EMERALD_ORE, 15);
        BLOCK_POINTS.put(Material.OBSIDIAN, 20);
    }

    private static final Material[] BLOCK_MATERIALS = BLOCK_POINTS.keySet().toArray(new Material[0]);

    public BreakingBlocksExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.BREAKING_BLOCKS; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.BREAKING_BLOCKS);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            giveKit(p);
            MessageUtil.sendRaw(p, "&eBreaking Blocks! Mine blocks for points!");
        }

        // Block regeneration
        Location arenaMin = arena.getConfig().getArenaMin();
        Location arenaMax = arena.getConfig().getArenaMax();
        if (arenaMin != null && arenaMax != null) {
            final Location min = arenaMin;
            final Location max = arenaMax;
            int regenInterval = config.getInt("regenerate-interval", 10) * 20;
            BukkitTask regenTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!running) { cancel(); return; }
                    // Randomly place ore blocks in arena
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    for (int i = 0; i < 5; i++) {
                        Location loc = com.thelab.plugin.utils.LocationUtil.randomInRegion(min, max);
                        Block block = loc.getBlock();
                        if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                            Material mat = BLOCK_MATERIALS[r.nextInt(BLOCK_MATERIALS.length)];
                            block.setType(mat, false);
                        }
                    }
                }
            }.runTaskTimer(TheLabPlugin.getInstance(), regenInterval, regenInterval);
            tasks.add(regenTask);
        }

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
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pick.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
            pick.setItemMeta(meta);
        }
        p.getInventory().setItem(0, pick);
        p.setGameMode(GameMode.SURVIVAL);
    }

    /** Called when a player breaks a block in the experiment. */
    public void handleBlockBreak(Player player, Block block) {
        if (!running || !arena.isPlayer(player)) return;
        Integer points = BLOCK_POINTS.get(block.getType());
        if (points == null) return;
        // Don't drop items - cancel drop and award points
        block.setType(Material.AIR, false);
        scoreManager.addScore(player.getUniqueId(), points);
        SoundUtil.playScore(player);
        MessageUtil.sendRaw(player, "&a+" + points + " pts!");
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
    }
}

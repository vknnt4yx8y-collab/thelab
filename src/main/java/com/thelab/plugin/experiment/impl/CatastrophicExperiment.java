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
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Catastrophic experiment.
 * Escalating disasters: TNT rain, lightning, lava pools, disappearing floor.
 */
public class CatastrophicExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<Location> lavaPlaced = new ArrayList<>();
    private final List<Block> removedBlocks = new ArrayList<>();
    private final Map<UUID, Long> eliminatedAt = new LinkedHashMap<>();
    private Runnable onEnd;
    private int elapsed = 0;

    public CatastrophicExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.CATASTROPHIC; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;
        elapsed = 0;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.CATASTROPHIC);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            giveKit(p);
            MessageUtil.sendRaw(p, "&eCatastrophic! Survive the escalating disasters!");
        }

        Location arenaMin = arena.getConfig().getArenaMin();
        Location arenaMax = arena.getConfig().getArenaMax();

        // Main disaster loop (every second)
        BukkitTask mainLoop = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                if (arena.getPlayers().isEmpty()) { cancel(); end(); onEnd.run(); return; }
                if (elapsed >= config.getDuration()) { cancel(); end(); onEnd.run(); return; }

                ThreadLocalRandom r = ThreadLocalRandom.current();

                if (arenaMin != null && arenaMax != null) {
                    // Phase 1 (0-30s): TNT rain
                    if (elapsed < 30 || elapsed >= 120) {
                        if (elapsed % 3 == 0) {
                            Location tntLoc = com.thelab.plugin.utils.LocationUtil.randomInRegion(arenaMin, arenaMax);
                            tntLoc.setY(tntLoc.getY() + 20);
                            if (tntLoc.getWorld() != null) {
                                // Particle warning
                                tntLoc.getWorld().spawnParticle(Particle.SMOKE, tntLoc, 20, 0.5, 1, 0.5, 0.1);
                                // Schedule actual TNT (knockback only, no block damage)
                                new BukkitRunnable() {
                                    @Override public void run() {
                                        if (!running) return;
                                        // Use explosion with no block damage
                                        Location groundLoc = tntLoc.clone();
                                        groundLoc.setY(arenaMin.getY() + 1);
                                        if (groundLoc.getWorld() != null) {
                                            groundLoc.getWorld().createExplosion(groundLoc, 2.0f, false, false);
                                        }
                                    }
                                }.runTaskLater(TheLabPlugin.getInstance(), 20L);
                            }
                        }
                    }

                    // Phase 2 (30-60s): Lightning
                    if (elapsed >= 30 && elapsed < 60 || elapsed >= 120) {
                        if (elapsed % 5 == 0) {
                            Location lightning = com.thelab.plugin.utils.LocationUtil.randomInRegion(arenaMin, arenaMax);
                            if (lightning.getWorld() != null) {
                                lightning.getWorld().strikeLightningEffect(lightning);
                                // Damage nearby players
                                for (Player p : arena.getPlayers()) {
                                    if (p.getLocation().distance(lightning) < 3) {
                                        p.damage(2.0);
                                    }
                                }
                            }
                        }
                    }

                    // Phase 3 (60-90s): Lava pools
                    if (elapsed >= 60 && elapsed < 90 || elapsed >= 120) {
                        if (elapsed % 10 == 0) {
                            Location lavaLoc = com.thelab.plugin.utils.LocationUtil.randomInRegion(arenaMin, arenaMax);
                            Block lavaBlock = lavaLoc.getBlock();
                            if (lavaBlock.getType() == Material.AIR) {
                                lavaBlock.setType(Material.LAVA, false);
                                lavaPlaced.add(lavaLoc);
                                // Remove after 5s
                                new BukkitRunnable() {
                                    @Override public void run() {
                                        if (lavaBlock.getType() == Material.LAVA) {
                                            lavaBlock.setType(Material.AIR, false);
                                        }
                                        lavaPlaced.remove(lavaLoc);
                                    }
                                }.runTaskLater(TheLabPlugin.getInstance(), 100L);
                            }
                        }
                    }

                    // Phase 4 (90s+): Floor disappearing
                    if (elapsed >= 90) {
                        if (r.nextInt(3) == 0) {
                            Location floorLoc = com.thelab.plugin.utils.LocationUtil.randomInRegion(arenaMin, arenaMax);
                            floorLoc.setY(arenaMin.getY());
                            Block floorBlock = floorLoc.getBlock();
                            if (!floorBlock.getType().isAir()) {
                                removedBlocks.add(floorBlock);
                                floorBlock.setType(Material.AIR, false);
                            }
                        }
                    }
                }

                // Check for player eliminations (fell below arena)
                int minY = (arenaMin != null) ? arenaMin.getBlockY() - 10 : -64;
                for (Player p : new ArrayList<>(arena.getPlayers())) {
                    if (p.getLocation().getY() < minY) {
                        eliminatedAt.put(p.getUniqueId(), (long) elapsed);
                        MessageUtil.sendRaw(p, "&cYou fell!");
                        SoundUtil.playElimination(p);
                        boolean shouldEnd = eliminate(p);
                        if (shouldEnd) { cancel(); end(); onEnd.run(); return; }
                    }
                }

                elapsed++;
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 20L, 20L);
        tasks.add(mainLoop);
    }

    private void giveKit(Player p) {
        p.getInventory().clear();
        p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        p.getInventory().setItem(0, new ItemStack(Material.GOLDEN_APPLE, 2));
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(20.0);
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();

        // Restore lava blocks
        for (Location loc : lavaPlaced) {
            if (loc.getBlock().getType() == Material.LAVA) {
                loc.getBlock().setType(Material.AIR, false);
            }
        }
        lavaPlaced.clear();

        // Score players by survival time (longer = better)
        List<Player> survivors = arena.getPlayers();
        for (Player p : survivors) {
            scoreManager.addScore(p.getUniqueId(), elapsed); // survived full time
        }
        // Eliminated players scored by when they were eliminated
        eliminatedAt.forEach((uuid, time) -> scoreManager.addScore(uuid, time.intValue()));
    }
}

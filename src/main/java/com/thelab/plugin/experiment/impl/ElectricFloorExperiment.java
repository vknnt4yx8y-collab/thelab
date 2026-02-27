package com.thelab.plugin.experiment.impl;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.Experiment;
import com.thelab.plugin.experiment.ExperimentConfig;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.game.ScoreManager;
import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Electric Floor experiment.
 * A safe color is announced; non-safe color blocks are removed; players on them fall.
 */
public class ElectricFloorExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private Runnable onEnd;
    private final DyeColor[] colors = DyeColor.values();
    private int round = 0;
    private int intervalTicks;

    public ElectricFloorExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
        this.intervalTicks = (int)(config.getDouble("color-change-interval", 3.0) * 20);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.ELECTRIC_FLOOR; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.ELECTRIC_FLOOR);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            MessageUtil.sendRaw(p, "&eElectric Floor! Stay on the announced color!");
        }

        // Color elimination rounds
        BukkitTask gameLoop = new BukkitRunnable() {
            int remaining = config.getDuration() * 20;

            @Override
            public void run() {
                if (!running || arena.getPlayers().size() <= 1) {
                    cancel();
                    end();
                    onEnd.run();
                    return;
                }
                if (remaining <= 0) {
                    cancel();
                    end();
                    onEnd.run();
                    return;
                }

                // Announce a new safe color every interval (accelerating)
                if (remaining % Math.max(20, intervalTicks - round * 5) == 0) {
                    DyeColor safeColor = colors[ThreadLocalRandom.current().nextInt(colors.length)];
                    arena.broadcastToPlayers("&eSafe color: &f" + safeColor.name().replace("_", " "));
                    MessageUtil.broadcastTitle(arena.getPlayers(),
                            "&eSafe: &f" + safeColor.name().replace("_", " "), "", 5, 30, 5);

                    // After grace period, remove unsafe color blocks and check players
                    int graceTicks = (int)(config.getDouble("grace-period", 1.5) * 20);
                    BukkitTask checkTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!running) { cancel(); return; }
                            eliminateOnUnsafe(safeColor);
                        }
                    }.runTaskLater(TheLabPlugin.getInstance(), graceTicks);
                    tasks.add(checkTask);
                    round++;
                }
                remaining--;
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 20L, 1L);
        tasks.add(gameLoop);
    }

    private void eliminateOnUnsafe(DyeColor safeColor) {
        // Find the material for the safe color (wool)
        Material safeMaterial = Material.valueOf(safeColor.name() + "_WOOL");

        for (Player p : new ArrayList<>(arena.getPlayers())) {
            Block below = p.getLocation().subtract(0, 1, 0).getBlock();
            if (below.getType() != safeMaterial) {
                MessageUtil.sendRaw(p, "&cYou were on the wrong color!");
                boolean shouldEnd = eliminate(p);
                if (shouldEnd) {
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

        List<Player> survivors = arena.getPlayers();
        if (!survivors.isEmpty()) {
            scoreManager.addScore(survivors.get(0).getUniqueId(), 3);
        }
    }
}

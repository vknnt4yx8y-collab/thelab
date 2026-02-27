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
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Splegg experiment.
 * Players use a shovel that shoots eggs; eggs break blocks. Fall below = eliminated.
 */
public class SpleggExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private Runnable onEnd;

    public SpleggExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.SPLEGG; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.SPLEGG);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            p.getInventory().clear();
            p.getInventory().setItem(0, new ItemStack(Material.IRON_SHOVEL));
            p.setGameMode(GameMode.SURVIVAL);
            MessageUtil.sendRaw(p, "&eSplegg! Right-click to shoot eggs and break blocks!");
        }

        // Fall detection: players below arena floor are eliminated
        Location arenaMin = arena.getConfig().getArenaMin();
        int floorY = arenaMin != null ? arenaMin.getBlockY() - 5 : -64;

        BukkitTask fallCheck = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                for (Player p : new ArrayList<>(arena.getPlayers())) {
                    if (p.getLocation().getY() < floorY) {
                        SoundUtil.playElimination(p);
                        MessageUtil.sendRaw(p, "&cYou fell out!");
                        arena.broadcast("&e" + p.getName() + " &cfell out!");
                        boolean end = eliminate(p);
                        if (end) { cancel(); end(); onEnd.run(); return; }
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 10L, 10L);
        tasks.add(fallCheck);

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

    /** Called when a player right-clicks with the shovel — shoots an egg. */
    public void handleShoot(Player player) {
        if (!running || !arena.isPlayer(player)) return;
        player.launchProjectile(Egg.class);
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

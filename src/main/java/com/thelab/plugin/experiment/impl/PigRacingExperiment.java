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
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pig Racing experiment.
 * Players ride saddled pigs around a track. First to complete all laps wins.
 */
public class PigRacingExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, Integer> playerLaps = new HashMap<>();
    private final Map<UUID, Pig> playerPigs = new HashMap<>();
    private final Map<UUID, Long> boostCooldown = new HashMap<>();
    private Runnable onEnd;

    public PigRacingExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.PIG_RACING; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        int totalLaps = config.getInt("laps", 3);
        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.PIG_RACING);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location loc = spawns.isEmpty() ? p.getLocation() : spawns.get(i % spawns.size());
            p.teleport(loc);
            p.getInventory().clear();
            p.getInventory().setItem(0, new ItemStack(Material.CARROT_ON_A_STICK));
            p.setGameMode(GameMode.SURVIVAL);
            playerLaps.put(p.getUniqueId(), 0);

            // Spawn pig and mount player
            if (loc.getWorld() != null) {
                Pig pig = loc.getWorld().spawn(loc, Pig.class, pg -> {
                    pg.setSaddle(true);
                    pg.setAI(false);
                });
                pig.addPassenger(p);
                playerPigs.put(p.getUniqueId(), pig);
            }
            MessageUtil.sendRaw(p, "&ePig Racing! Complete " + totalLaps + " laps first!");
            MessageUtil.sendRaw(p, "&7Right-click to boost your pig!");
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

    /** Called when a player uses carrot on a stick (boost). */
    public void handleBoost(Player player) {
        if (!running || !arena.isPlayer(player)) return;
        long now = System.currentTimeMillis();
        long cooldownMs = config.getInt("boost-cooldown", 3) * 1000L;
        if (now - boostCooldown.getOrDefault(player.getUniqueId(), 0L) < cooldownMs) {
            MessageUtil.sendRaw(player, "&cBoost on cooldown!");
            return;
        }
        boostCooldown.put(player.getUniqueId(), now);
        Pig pig = playerPigs.get(player.getUniqueId());
        if (pig != null) {
            pig.setVelocity(pig.getVelocity().add(
                    player.getLocation().getDirection().multiply(1.5)));
        }
        SoundUtil.playScore(player);
        MessageUtil.sendRaw(player, "&aBoost!");
    }

    /** Called when a player crosses a checkpoint (lap). */
    public void handleLapComplete(Player player) {
        if (!running || !arena.isPlayer(player)) return;
        int totalLaps = config.getInt("laps", 3);
        int laps = playerLaps.merge(player.getUniqueId(), 1, Integer::sum);
        arena.broadcastToPlayers("&e" + player.getName() + " &acompleted lap " + laps + "/" + totalLaps + "!");
        if (laps >= totalLaps) {
            // Winner!
            scoreManager.addScore(player.getUniqueId(), 10);
            arena.broadcastToPlayers("&6" + player.getName() + " &6finished the race!");
            SoundUtil.playScore(player);
            end();
            if (onEnd != null) onEnd.run();
        }
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        for (Pig pig : playerPigs.values()) {
            if (pig.isValid()) pig.remove();
        }
        playerPigs.clear();
        // Score remaining players by laps completed
        playerLaps.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .forEach(e -> scoreManager.addScore(e.getKey(), e.getValue()));
    }
}

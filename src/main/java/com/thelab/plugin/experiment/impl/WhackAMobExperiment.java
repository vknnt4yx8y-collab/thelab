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
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Whack-A-Mob experiment.
 * Mobs spawn at pads, score differs by mob type.
 */
public class WhackAMobExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, Integer> activeMobs = new HashMap<>();
    private Runnable onEnd;

    // Mob type -> point value (negative = penalty)
    private static final Map<EntityType, Integer> MOB_POINTS = new EnumMap<>(EntityType.class);
    static {
        MOB_POINTS.put(EntityType.PIG, 1);
        MOB_POINTS.put(EntityType.COW, 1);
        MOB_POINTS.put(EntityType.CHICKEN, 2);
        MOB_POINTS.put(EntityType.SHEEP, 3);
        MOB_POINTS.put(EntityType.ZOMBIE, 5);
        MOB_POINTS.put(EntityType.SKELETON, -2);
        MOB_POINTS.put(EntityType.CREEPER, -3);
    }

    private static final EntityType[] MOB_TYPES = MOB_POINTS.keySet().toArray(new EntityType[0]);

    public WhackAMobExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.WHACK_A_MOB; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.WHACK_A_MOB);
        List<Location> padSpawns = spawns.size() > players.size() ? spawns.subList(players.size(), spawns.size()) : new ArrayList<>();
        List<Location> playerSpawns = spawns.isEmpty() ? new ArrayList<>() : spawns.subList(0, Math.min(players.size(), spawns.size()));

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!playerSpawns.isEmpty()) p.teleport(playerSpawns.get(i % playerSpawns.size()));
            giveKit(p);
            MessageUtil.sendRaw(p, "&eWhack-A-Mob! Hit the right mobs for points!");
        }

        // Spawn mobs on pads
        int stayMin = config.getInt("mob-stay-min", 3) * 20;
        int stayMax = config.getInt("mob-stay-max", 5) * 20;

        BukkitTask spawnTask = new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                tick++;
                if (padSpawns.isEmpty()) return;
                // Spawn mob at random pad
                Location pad = padSpawns.get(ThreadLocalRandom.current().nextInt(padSpawns.size()));
                if (pad.getWorld() == null) return;
                EntityType type = MOB_TYPES[ThreadLocalRandom.current().nextInt(MOB_TYPES.length)];
                Entity mob = pad.getWorld().spawnEntity(pad, type);
                mob.setPersistent(false);
                ((LivingEntity) mob).setAI(false);
                activeMobs.put(mob.getUniqueId(), MOB_POINTS.getOrDefault(type, 1));

                // Despawn after stay time
                int stay = stayMin + ThreadLocalRandom.current().nextInt(stayMax - stayMin + 1);
                new BukkitRunnable() {
                    @Override public void run() {
                        if (mob.isValid()) {
                            mob.remove();
                            activeMobs.remove(mob.getUniqueId());
                        }
                    }
                }.runTaskLater(TheLabPlugin.getInstance(), stay);
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 20L, 30L);
        tasks.add(spawnTask);

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
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        p.getInventory().setItem(0, sword);
        p.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));
        p.setGameMode(GameMode.ADVENTURE);
    }

    /** Called when a player hits a mob. */
    public void handleMobHit(Player player, Entity mob) {
        if (!running || !arena.isPlayer(player)) return;
        Integer points = activeMobs.remove(mob.getUniqueId());
        if (points == null) return;
        mob.remove();

        if (mob.getType() == EntityType.CREEPER) {
            // Particle explosion effect
            mob.getLocation().getWorld().spawnParticle(
                    Particle.EXPLOSION, mob.getLocation(), 5, 1, 1, 1, 0.1);
        }

        scoreManager.addScore(player.getUniqueId(), points);
        SoundUtil.playScore(player);
        if (points > 0) {
            MessageUtil.sendRaw(player, "&a+" + points + " points!");
        } else {
            MessageUtil.sendRaw(player, "&c" + points + " points (penalty)!");
        }
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        for (UUID uid : activeMobs.keySet()) {
            Entity e = Bukkit.getEntity(uid);
            if (e != null) e.remove();
        }
        activeMobs.clear();
    }
}

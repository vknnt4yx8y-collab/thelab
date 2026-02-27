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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fight experiment.
 * Standard deathmatch PvP. Kill = 1 point. Power-ups spawn every 20s.
 */
public class FightExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<Item> powerUps = new ArrayList<>();
    private Runnable onEnd;

    public FightExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.FIGHT; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.FIGHT);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            giveKit(p);
            MessageUtil.sendRaw(p, "&eFight! Most kills wins!");
        }

        Location arenaMin = arena.getConfig().getArenaMin();
        Location arenaMax = arena.getConfig().getArenaMax();

        // Power-up spawner
        if (arenaMin != null && arenaMax != null) {
            final Location min = arenaMin;
            final Location max = arenaMax;
            int powerupInterval = config.getInt("powerup-interval", 20) * 20;
            BukkitTask powerupTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!running) { cancel(); return; }
                    spawnPowerUp(min, max);
                }
            }.runTaskTimer(TheLabPlugin.getInstance(), powerupInterval, powerupInterval);
            tasks.add(powerupTask);
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
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            sword.setItemMeta(meta);
        }
        p.getInventory().setItem(0, sword);
        p.getInventory().setItem(1, new ItemStack(Material.BOW));
        p.getInventory().setItem(9, new ItemStack(Material.ARROW, 16));
        p.getInventory().setItem(8, new ItemStack(Material.GOLDEN_APPLE, 2));
        p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setGameMode(GameMode.SURVIVAL);
    }

    private void spawnPowerUp(Location min, Location max) {
        if (min.getWorld() == null) return;
        Location loc = com.thelab.plugin.utils.LocationUtil.randomInRegion(min, max);
        loc.setY(loc.getY() + 1);
        Material[] powerUps = {Material.GOLDEN_APPLE, Material.GOLDEN_APPLE, Material.GOLDEN_APPLE};
        Material mat = powerUps[ThreadLocalRandom.current().nextInt(powerUps.length)];
        Item item = loc.getWorld().dropItem(loc, new ItemStack(mat));
        item.setPickupDelay(0);
        this.powerUps.add(item);
    }

    /** Called when a player kills another player. */
    public void handleKill(Player killer, Player victim) {
        if (!running || !arena.isPlayer(killer)) return;
        scoreManager.addScore(killer.getUniqueId(), 1);
        SoundUtil.playScore(killer);
        MessageUtil.sendRaw(killer, "&a+1 kill point!");
        arena.broadcastToPlayers("&e" + killer.getName() + " &7killed &e" + victim.getName() + "!");

        // Respawn after delay
        int respawnDelay = config.getInt("respawn-delay", 3) * 20;
        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.FIGHT);
        if (!spawns.isEmpty()) {
            Location respawn = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
            new BukkitRunnable() {
                @Override public void run() {
                    if (victim.isOnline() && arena.isPlayer(victim)) {
                        victim.teleport(respawn);
                        victim.setHealth(20.0);
                        victim.setFoodLevel(20);
                        giveKit(victim);
                        MessageUtil.sendRaw(victim, "&aYou respawned!");
                    }
                }
            }.runTaskLater(TheLabPlugin.getInstance(), respawnDelay);
        }
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        powerUps.forEach(Item::remove);
        powerUps.clear();
    }
}

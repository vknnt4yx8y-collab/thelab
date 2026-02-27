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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Snowman experiment.
 * Players freeze each other with snowballs. Frozen players can be unfrozen by teammates.
 * Scoring: +1 per freeze, -0.5 for being frozen.
 */
public class SnowmanExperiment extends Experiment {

    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, Long> frozenUntil = new HashMap<>();
    private Runnable onEnd;

    public SnowmanExperiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        super(arena, config, scoreManager);
    }

    @Override
    public ExperimentType getType() { return ExperimentType.SNOWMAN; }

    @Override
    public void start(List<Player> players, Runnable onEnd) {
        this.onEnd = onEnd;
        this.running = true;

        List<Location> spawns = arena.getConfig().getSpawnsFor(ExperimentType.SNOWMAN);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!spawns.isEmpty()) p.teleport(spawns.get(i % spawns.size()));
            giveKit(p);
            MessageUtil.sendRaw(p, "&eSnowman! Freeze opponents with snowballs!");
        }

        int freezeDuration = config.getInt("freeze-duration", 5);
        int unfrezeRadius = config.getInt("unfreeze-radius", 2);

        // Freeze check loop
        BukkitTask freezeLoop = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                long now = System.currentTimeMillis();
                // Unfreeze expired players
                for (Player p : arena.getPlayers()) {
                    Long until = frozenUntil.get(p.getUniqueId());
                    if (until != null && now > until) {
                        frozenUntil.remove(p.getUniqueId());
                        p.removePotionEffect(PotionEffectType.SLOWNESS);
                        MessageUtil.sendRaw(p, "&aYou have thawed!");
                    }
                }
                // Replenish snowballs
                for (Player p : arena.getPlayers()) {
                    if (frozenUntil.containsKey(p.getUniqueId())) continue;
                    ItemStack balls = p.getInventory().getItem(0);
                    if (balls == null || balls.getType() != Material.SNOWBALL || balls.getAmount() < 8) {
                        p.getInventory().setItem(0, new ItemStack(Material.SNOWBALL, 16));
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 10L, 10L);
        tasks.add(freezeLoop);

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
        p.getInventory().setItem(0, new ItemStack(Material.SNOWBALL, 16));
        // Snowman look: white leather armor, carved pumpkin head
        ItemStack helmet = new ItemStack(Material.CARVED_PUMPKIN);
        p.getInventory().setHelmet(helmet);
        ItemStack chestplate = colorLeather(Material.LEATHER_CHESTPLATE, Color.WHITE);
        ItemStack leggings = colorLeather(Material.LEATHER_LEGGINGS, Color.WHITE);
        ItemStack boots = colorLeather(Material.LEATHER_BOOTS, Color.WHITE);
        p.getInventory().setChestplate(chestplate);
        p.getInventory().setLeggings(leggings);
        p.getInventory().setBoots(boots);
        p.setGameMode(GameMode.ADVENTURE);
    }

    private ItemStack colorLeather(Material mat, Color color) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) { meta.setColor(color); item.setItemMeta(meta); }
        return item;
    }

    /** Called when a player is hit by a snowball from another arena player. */
    public void handleSnowballHit(Player victim, Player thrower) {
        if (!running || !arena.isPlayer(victim) || !arena.isPlayer(thrower)) return;
        if (frozenUntil.containsKey(victim.getUniqueId())) return; // Already frozen

        int freezeSecs = config.getInt("freeze-duration", 5);
        frozenUntil.put(victim.getUniqueId(), System.currentTimeMillis() + freezeSecs * 1000L);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeSecs * 20, 127, false, false));
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
        MessageUtil.sendRaw(victim, "&bYou are frozen for " + freezeSecs + " seconds!");
        MessageUtil.sendRaw(thrower, "&a+" + 1 + " freeze point!");
        scoreManager.addScore(thrower.getUniqueId(), 1);
        SoundUtil.playScore(thrower);
    }

    @Override
    public void end() {
        running = false;
        tasks.forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        tasks.clear();
        frozenUntil.clear();
    }
}

package com.thelab.plugin.listeners;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import com.thelab.plugin.experiment.impl.*;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** Handles arena-specific gameplay events. */
public class ArenaProtectionListener implements Listener {

    private final TheLabPlugin plugin;

    public ArenaProtectionListener(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) return;

        plugin.getArenaManager().getArenaForPlayer(shooter).ifPresent(arena -> {
            if (arena.getState() != ArenaState.EXPERIMENT_PLAY) return;

            plugin.getGameManager().getActiveExperiment(arena).ifPresent(exp -> {
                // Dodge Ball: snowball hits player
                if (exp instanceof DodgeBallExperiment dbe
                        && event.getEntity() instanceof Snowball
                        && event.getHitEntity() instanceof Player victim
                        && arena.isPlayer(victim)) {
                    dbe.handleHit(victim);
                }

                // Balloon Pop: arrow hits balloon entity
                if (exp instanceof BalloonPopExperiment bpe
                        && event.getHitEntity() != null) {
                    bpe.handleBalloonHit(shooter, event.getHitEntity());
                }
            });
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Entity target)) return;

        plugin.getArenaManager().getArenaForPlayer(attacker).ifPresent(arena -> {
            if (arena.getState() != ArenaState.EXPERIMENT_PLAY) return;

            plugin.getGameManager().getActiveExperiment(arena).ifPresent(exp -> {
                // Whack-A-Mob: hit mob
                if (exp instanceof WhackAMobExperiment wame
                        && !(target instanceof Player)) {
                    event.setCancelled(true);
                    wame.handleMobHit(attacker, target);
                }

                // Snowman: snowball (handled via projectile) - for direct hits, check damage by snowball
                if (exp instanceof SnowmanExperiment sne
                        && target instanceof Player victim
                        && arena.isPlayer(victim)) {
                    // Direct snowball hit handled in ProjectileHitEvent
                }
            });
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        plugin.getArenaManager().getArenaForPlayer(player).ifPresent(arena -> {
            if (arena.getState() != ArenaState.EXPERIMENT_PLAY) return;

            plugin.getGameManager().getActiveExperiment(arena).ifPresent(exp -> {
                // Splegg: right-click with shovel
                if (exp instanceof SpleggExperiment se
                        && event.getItem() != null
                        && event.getItem().getType() == Material.IRON_SHOVEL
                        && event.getAction().name().startsWith("RIGHT")) {
                    event.setCancelled(true);
                    se.handleShoot(player);
                }

                // Pig Racing: right-click with carrot on a stick
                if (exp instanceof PigRacingExperiment pre
                        && event.getItem() != null
                        && event.getItem().getType() == Material.CARROT_ON_A_STICK
                        && event.getAction().name().startsWith("RIGHT")) {
                    pre.handleBoost(player);
                }
            });
        });
    }
}

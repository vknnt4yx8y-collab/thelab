package com.thelab.plugin.listeners;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.entity.Player;

/** Protects players and arenas from unwanted interactions. */
public class PlayerProtectionListener implements Listener {

    private final TheLabPlugin plugin;

    public PlayerProtectionListener(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.getArenaManager().getArenaForPlayer(player).ifPresent(arena -> {
            if (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING
                    || arena.getState() == ArenaState.EXPERIMENT_INTRO) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        plugin.getArenaManager().getArenaForPlayer(victim).ifPresent(arena -> {
            ArenaState state = arena.getState();
            // Cancel PvP in lobby/starting phases
            if (state == ArenaState.WAITING || state == ArenaState.STARTING
                    || state == ArenaState.EXPERIMENT_INTRO || state == ArenaState.EXPERIMENT_RESULTS
                    || state == ArenaState.GAME_END) {
                event.setCancelled(true);
            }
            // Cancel PvP in Gold Rush (no PvP experiment)
            if (state == ArenaState.EXPERIMENT_PLAY) {
                plugin.getGameManager().getActiveExperiment(arena).ifPresent(exp -> {
                    if (exp.getType() == com.thelab.plugin.experiment.ExperimentType.GOLD_RUSH
                            || exp.getType() == com.thelab.plugin.experiment.ExperimentType.CRAZY_PAINTS
                            || exp.getType() == com.thelab.plugin.experiment.ExperimentType.BALLOON_POP) {
                        event.setCancelled(true);
                    }
                });
            }
        });
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        plugin.getArenaManager().getArenaForPlayer(player).ifPresent(arena -> {
            ArenaState state = arena.getState();
            if (state == ArenaState.WAITING || state == ArenaState.STARTING
                    || state == ArenaState.EXPERIMENT_INTRO || state == ArenaState.EXPERIMENT_RESULTS
                    || state == ArenaState.GAME_END) {
                event.setCancelled(true);
                return;
            }
            if (state == ArenaState.EXPERIMENT_PLAY) {
                // Allow breaking only in certain experiments
                plugin.getGameManager().getActiveExperiment(arena).ifPresent(exp -> {
                    if (exp instanceof com.thelab.plugin.experiment.impl.BreakingBlocksExperiment bbe) {
                        event.setCancelled(true);
                        event.setDropItems(false);
                        bbe.handleBlockBreak(player, event.getBlock());
                    } else if (exp.getType() != com.thelab.plugin.experiment.ExperimentType.SPLEGG) {
                        event.setCancelled(true);
                    }
                });
            }
        });
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        plugin.getArenaManager().getArenaForPlayer(player).ifPresent(arena -> {
            if (arena.getState() != ArenaState.EXPERIMENT_PLAY) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        plugin.getArenaManager().getArenaForPlayer(event.getPlayer()).ifPresent(arena -> {
            if (arena.getState() != ArenaState.EXPERIMENT_PLAY) {
                event.setCancelled(true);
            }
        });
    }
}

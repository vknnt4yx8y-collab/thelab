package com.thelab.plugin.listeners;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import com.thelab.plugin.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Handles player join/quit and combat events. */
public class PlayerConnectionListener implements Listener {

    private final TheLabPlugin plugin;

    public PlayerConnectionListener(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().handleJoin(event.getPlayer());
        plugin.getStatsManager().loadStats(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerManager().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        plugin.getArenaManager().getArenaForPlayer(victim).ifPresent(arena -> {
            event.setKeepInventory(true);
            event.setDroppedExp(0);
            event.getDrops().clear();

            // Check if there's a killer
            if (event.getEntity().getKiller() != null) {
                Player killer = event.getEntity().getKiller();
                plugin.getGameManager().getActiveExperiment(arena).ifPresent(exp -> {
                    if (exp instanceof com.thelab.plugin.experiment.impl.FightExperiment fe) {
                        fe.handleKill(killer, victim);
                    }
                });
            }
        });
    }
}

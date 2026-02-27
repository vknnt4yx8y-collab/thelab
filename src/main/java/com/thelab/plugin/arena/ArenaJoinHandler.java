package com.thelab.plugin.arena;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import com.thelab.plugin.player.LabPlayer;
import com.thelab.plugin.player.PlayerState;
import com.thelab.plugin.utils.LocationUtil;
import com.thelab.plugin.utils.MessageUtil;
import com.thelab.plugin.utils.SoundUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Handles the logic for joining and leaving arenas. */
public class ArenaJoinHandler {

    private final TheLabPlugin plugin;

    public ArenaJoinHandler(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    /** Attempts to make a player join an arena. */
    public void joinArena(Player player, Arena arena) {
        // Validation
        if (!arena.getConfig().isEnabled()) {
            MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.arena-disabled"));
            return;
        }
        if (plugin.getArenaManager().getArenaForPlayer(player).isPresent()) {
            MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.already-in-arena"));
            return;
        }
        if (!arena.isJoinable()) {
            MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.arena-full"));
            return;
        }
        if (!arena.getConfig().isFullySetup()) {
            MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.arena-not-setup"));
            return;
        }

        // Save player state and add to arena
        LabPlayer lp = plugin.getPlayerManager().getLabPlayer(player);
        lp.joinArena(arena, player);
        arena.addPlayer(player);

        // Teleport to lobby spawn
        Location lobby = arena.getLobbySpawn();
        if (lobby != null) player.teleport(LocationUtil.center(lobby));

        // Give lobby items
        giveLobbyItems(player);

        // Notify
        MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("arena.join", "arena", arena.getDisplayName()));
        arena.broadcast(plugin.getConfigManager().getMessage("arena.player-joined",
                "player", player.getName(),
                "current", String.valueOf(arena.getPlayerCount()),
                "max", String.valueOf(arena.getMaxPlayers())));
        SoundUtil.play(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);

        // Check if we should start countdown
        if (arena.getPlayerCount() >= arena.getMinPlayers()
                && arena.getState() == ArenaState.WAITING) {
            plugin.getGameManager().startCountdown(arena);
        }

        // Update scoreboard
        plugin.getScoreboardManager().updateScoreboard(player, arena);

        // Start narrator lobby messages
        if (arena.getPlayerCount() == 1) {
            plugin.getNarratorManager().startLobbyMessages(arena);
        }
    }

    /** Makes a player leave their current arena. */
    public void leaveArena(Player player) {
        plugin.getArenaManager().getArenaForPlayer(player).ifPresentOrElse(arena -> {
            doLeave(player, arena);
        }, () -> {
            MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.not-in-arena"));
        });
    }

    public void doLeave(Player player, Arena arena) {
        boolean wasPlayer = arena.isPlayer(player);
        arena.removePlayer(player);
        arena.removeSpectator(player);

        LabPlayer lp = plugin.getPlayerManager().getLabPlayer(player);
        lp.leaveArena();

        MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("arena.leave"));
        if (wasPlayer) {
            arena.broadcast(plugin.getConfigManager().getMessage("arena.player-left",
                    "player", player.getName(),
                    "current", String.valueOf(arena.getPlayerCount()),
                    "max", String.valueOf(arena.getMaxPlayers())));
        }

        // Teleport to main lobby
        Location mainLobby = LocationUtil.fromConfig(
                plugin.getConfigManager().getConfig().getConfigurationSection("main-lobby"));
        if (mainLobby != null && mainLobby.getWorld() != null) {
            player.teleport(mainLobby);
        }

        // Cancel countdown if not enough players
        if (arena.getState() == ArenaState.STARTING
                && arena.getPlayerCount() < arena.getMinPlayers()) {
            plugin.getGameManager().cancelCountdown(arena);
        }

        plugin.getScoreboardManager().removeScoreboard(player);
    }

    /** Gives a player the lobby items. */
    private void giveLobbyItems(Player player) {
        player.getInventory().setItem(0,
                new ItemStack(Material.COMPASS)); // Map Selector
        player.getInventory().setItem(4,
                new ItemStack(Material.NETHER_STAR)); // Leave
        player.getInventory().setItem(8,
                new ItemStack(Material.CLOCK)); // Stats
    }
}

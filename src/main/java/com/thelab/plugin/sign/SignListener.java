package com.thelab.plugin.sign;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/** Handles click events on TheLab signs and sign creation. */
public class SignListener implements Listener {

    private final TheLabPlugin plugin;

    public SignListener(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (!event.getPlayer().hasPermission("thelab.admin")) return;
        String line0 = event.line(0) != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.line(0))
                : "";
        if (!line0.equalsIgnoreCase("[TheLab]")) return;

        String line1 = event.line(1) != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.line(1))
                : "";
        String arenaId = line1.trim();
        if (arenaId.isEmpty()) return;

        plugin.getArenaManager().getArena(arenaId).ifPresent(arena -> {
            plugin.getSignManager().addSign(event.getBlock().getLocation(), arenaId);
            com.thelab.plugin.utils.MessageUtil.sendRaw(event.getPlayer(),
                    "&aSign created for arena &e" + arenaId);
        });
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign)) return;
        Player player = event.getPlayer();
        String arenaId = plugin.getSignManager().getArenaId(event.getClickedBlock().getLocation());
        if (arenaId == null) return;

        event.setCancelled(true);
        // Join arena
        plugin.getArenaManager().getArena(arenaId).ifPresentOrElse(
                arena -> plugin.getArenaJoinHandler().joinArena(player, arena),
                () -> com.thelab.plugin.utils.MessageUtil.sendRaw(player,
                        "&cArena not found: " + arenaId));
    }
}

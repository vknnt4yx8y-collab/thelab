package com.thelab.plugin.listeners;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.ArenaSetupWizard;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/** Listener for arena setup wizard interactions. */
public class WorldProtectionListener implements Listener {

    private final TheLabPlugin plugin;

    public WorldProtectionListener(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        plugin.getSetupWizard().ifPresent(wizard -> {
            if (wizard.isInWizard(event.getPlayer())) {
                event.setCancelled(true);
                // Must run wizard logic on main thread
                String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        wizard.handleChat(event.getPlayer(), msg));
            }
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        plugin.getSetupWizard().ifPresent(wizard -> {
            if (wizard.isInWizard(event.getPlayer()) && event.getClickedBlock() != null) {
                boolean handled = wizard.handleInteract(event.getPlayer(),
                        event.getAction(), event.getClickedBlock().getLocation());
                if (handled) event.setCancelled(true);
            }
        });
    }
}

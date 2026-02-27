package com.thelab.plugin.listeners;

import com.thelab.plugin.TheLabPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/** Handles arena chat isolation. */
public class ChatListener implements Listener {

    private final TheLabPlugin plugin;

    public ChatListener(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfigManager().isArenaChatOnly()) return;

        plugin.getArenaManager().getArenaForPlayer(event.getPlayer()).ifPresent(arena -> {
            // Restrict chat recipients to arena participants only
            event.viewers().clear();
            event.viewers().addAll(arena.getAllParticipants());
            // Prefix message with arena tag
            String msgText = PlainTextComponentSerializer.plainText().serialize(event.message());
            event.message(Component.text("[" + arena.getDisplayName() + "] "
                    + event.getPlayer().getName() + ": " + msgText));
        });
    }
}

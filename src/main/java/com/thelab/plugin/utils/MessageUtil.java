package com.thelab.plugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;

/** Utility methods for sending messages to players. */
public final class MessageUtil {

    private MessageUtil() {}

    /** Translates '&amp;' color codes in a string. */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Strips color codes from a string. */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }

    /** Converts a colored string to an Adventure Component. */
    public static Component toComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    /** Sends a colorized message to a player (no prefix). */
    public static void sendRaw(Player player, String message) {
        player.sendMessage(toComponent(message));
    }

    /** Sends a title to a player. */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                toComponent(title),
                toComponent(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    /** Sends an action bar message to a player. */
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(toComponent(message));
    }

    /** Broadcasts a message to a collection of players. */
    public static void broadcast(Collection<? extends Player> players, String message) {
        Component comp = toComponent(message);
        for (Player p : players) {
            p.sendMessage(comp);
        }
    }

    /** Broadcasts a title to a collection of players. */
    public static void broadcastTitle(Collection<? extends Player> players, String title, String subtitle,
                                       int fadeIn, int stay, int fadeOut) {
        Title t = Title.title(
                toComponent(title),
                toComponent(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );
        for (Player p : players) {
            p.showTitle(t);
        }
    }

    /** Broadcasts an action bar to all players in collection. */
    public static void broadcastActionBar(Collection<? extends Player> players, String message) {
        Component comp = toComponent(message);
        for (Player p : players) {
            p.sendActionBar(comp);
        }
    }

    /** Formats seconds as MM:SS. */
    public static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}

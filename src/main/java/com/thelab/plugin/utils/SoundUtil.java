package com.thelab.plugin.utils;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Utility for playing sounds to players. */
public final class SoundUtil {

    private SoundUtil() {}

    /** Plays a sound to a player. */
    public static void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    /** Plays a sound by name string to a player (safe, falls back silently on invalid name). */
    public static void play(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            play(player, sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            // Unknown sound name — silently skip
        }
    }

    /** Plays a sound to all players in a collection. */
    public static void playToAll(Collection<? extends Player> players, Sound sound, float volume, float pitch) {
        for (Player p : players) {
            play(p, sound, volume, pitch);
        }
    }

    /** Plays the countdown tick sound to a player. */
    public static void playCountdownTick(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
    }

    /** Plays the countdown final sound to a player. */
    public static void playCountdownFinal(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    /** Plays the elimination sound to a player. */
    public static void playElimination(Player player) {
        play(player, Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
    }

    /** Plays a point scored sound to a player. */
    public static void playScore(Player player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }
}

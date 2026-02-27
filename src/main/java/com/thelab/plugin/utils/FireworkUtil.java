package com.thelab.plugin.utils;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

/** Utility for launching fireworks. */
public final class FireworkUtil {

    private FireworkUtil() {}

    /** Launches a firework at the given location with specified colors. */
    public static void launch(Location loc, Color... colors) {
        if (loc == null || loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        FireworkEffect.Builder builder = FireworkEffect.builder()
                .flicker(false)
                .trail(true)
                .with(FireworkEffect.Type.BALL_LARGE);
        for (Color c : colors) {
            builder.withColor(c);
        }
        meta.addEffect(builder.build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.detonate();
    }

    /** Launches a celebration of fireworks at the given location over 2 seconds. */
    public static void launchCelebration(Plugin plugin, Location loc) {
        if (loc == null || plugin == null) return;
        Color[] palette = {
                Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
                Color.AQUA, Color.BLUE, Color.PURPLE, Color.FUCHSIA
        };
        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 8) {
                    cancel();
                    return;
                }
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Location offset = loc.clone().add(
                        r.nextDouble(-3, 3), 0, r.nextDouble(-3, 3));
                launch(offset, palette[count % palette.length]);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}

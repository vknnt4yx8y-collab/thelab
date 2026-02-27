package com.thelab.plugin.utils;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/** Utility for running countdown timers via Bukkit scheduler. */
public final class CountdownUtil {

    private CountdownUtil() {}

    /**
     * Starts a countdown from {@code seconds} to 0.
     * {@code onTick} is called each second with the remaining time.
     * {@code onFinish} is called when the countdown reaches 0.
     *
     * @return the BukkitTask, can be cancelled to stop the countdown
     */
    public static BukkitTask runCountdown(Plugin plugin, int seconds,
                                          Consumer<Integer> onTick, Runnable onFinish) {
        return new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining > 0) {
                    onTick.accept(remaining);
                    remaining--;
                } else {
                    cancel();
                    onFinish.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}

package com.thelab.plugin.game;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/** A countdown timer backed by the Bukkit scheduler. */
public class GameTimer {

    private int remaining;
    private BukkitTask task;
    private boolean running;

    public void start(Plugin plugin, int seconds, Consumer<Integer> onTick, Runnable onFinish) {
        stop();
        remaining = seconds;
        running = true;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (remaining > 0) {
                    onTick.accept(remaining);
                    remaining--;
                } else {
                    running = false;
                    cancel();
                    onFinish.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            try { task.cancel(); } catch (Exception ignored) {}
            task = null;
        }
        running = false;
    }

    public boolean isRunning() { return running; }
    public int getRemaining() { return remaining; }
}

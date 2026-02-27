package com.thelab.plugin.narrator;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Dr. Zuk narrator - sends themed messages during game events. */
public class DrZuk {

    private static final String PREFIX = "&8[&aDr. Zuk&8] &f";

    private final TheLabPlugin plugin;
    private final Map<String, BukkitTask> lobbyTasks = new HashMap<>();

    public DrZuk(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sends a narrator message to all players in the arena. */
    public void broadcast(Arena arena, String message) {
        arena.broadcast(PREFIX + message);
    }

    /** Sends a random message from a list to the arena. */
    public void sendRandom(Arena arena, List<String> messages) {
        if (messages.isEmpty()) return;
        String msg = messages.get(new Random().nextInt(messages.size()));
        broadcast(arena, msg);
    }

    /** Starts periodic lobby wait messages. */
    public void startLobbyMessages(Arena arena) {
        stopLobbyMessages(arena);
        List<String> msgs = plugin.getConfigManager().getNarratorMessages("lobby-wait");
        BukkitTask task = new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (arena.getPlayers().isEmpty()) return;
                broadcast(arena, msgs.get(index % msgs.size()));
                index++;
            }
        }.runTaskTimer(plugin, 200L, 400L); // 10s delay, every 20s
        lobbyTasks.put(arena.getId(), task);
    }

    /** Stops lobby messages for an arena. */
    public void stopLobbyMessages(Arena arena) {
        BukkitTask task = lobbyTasks.remove(arena.getId());
        if (task != null) task.cancel();
    }

    /** Sends countdown narrator messages. */
    public void sendCountdownMessage(Arena arena) {
        List<String> msgs = plugin.getConfigManager().getNarratorMessages("countdown");
        sendRandom(arena, msgs);
    }

    /** Sends experiment intro narrator message. */
    public void sendExperimentIntro(Arena arena, ExperimentType type) {
        List<String> msgs = plugin.getConfigManager().getNarratorMessages(
                "experiment-intro." + type.getConfigKey());
        sendRandom(arena, msgs);
    }

    /** Sends experiment end narrator message. */
    public void sendExperimentEnd(Arena arena) {
        List<String> msgs = plugin.getConfigManager().getNarratorMessages("experiment-end");
        sendRandom(arena, msgs);
    }

    /** Sends game end narrator message. */
    public void sendGameEnd(Arena arena) {
        List<String> msgs = plugin.getConfigManager().getNarratorMessages("game-end");
        sendRandom(arena, msgs);
        stopLobbyMessages(arena);
    }

    /** Shuts down all narrator tasks. */
    public void shutdown() {
        lobbyTasks.values().forEach(t -> { try { t.cancel(); } catch (Exception ignored) {} });
        lobbyTasks.clear();
    }
}

package com.thelab.plugin.experiment;

import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.game.ScoreManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Base class for all TheLab experiments.
 * Subclasses override lifecycle methods and register Bukkit listeners.
 */
public abstract class Experiment {

    protected final Arena arena;
    protected final ExperimentConfig config;
    protected final ScoreManager scoreManager;
    protected boolean running;

    public Experiment(Arena arena, ExperimentConfig config, ScoreManager scoreManager) {
        this.arena = arena;
        this.config = config;
        this.scoreManager = scoreManager;
        this.running = false;
    }

    /** Returns the type of this experiment. */
    public abstract ExperimentType getType();

    /**
     * Called when the experiment starts.
     * Give kits, teleport players, start timers, spawn entities, etc.
     *
     * @param players all active (non-spectator) players
     * @param onEnd   callback to invoke when the experiment is over
     */
    public abstract void start(List<Player> players, Runnable onEnd);

    /**
     * Called when the experiment ends (time out or win condition).
     * Clean up entities, cancel tasks, clear inventories.
     */
    public abstract void end();

    /**
     * Eliminates a player from the experiment (moves to spectator).
     * @return true if the experiment should end (no players remaining)
     */
    public boolean eliminate(Player player) {
        arena.removePlayer(player);
        arena.addSpectator(player);
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        return arena.getPlayers().size() <= 1;
    }

    /** Returns whether this experiment is currently running. */
    public boolean isRunning() { return running; }

    /** Returns the duration of this experiment in seconds. */
    public int getDuration() { return config.getDuration(); }

    /** Returns the arena this experiment is running in. */
    public Arena getArena() { return arena; }

    /** Returns the score manager for this experiment. */
    public ScoreManager getScoreManager() { return scoreManager; }
}

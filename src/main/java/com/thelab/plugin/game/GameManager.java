package com.thelab.plugin.game;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaResetHandler;
import com.thelab.plugin.arena.ArenaState;
import com.thelab.plugin.experiment.Experiment;
import com.thelab.plugin.experiment.ExperimentRegistry;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.player.LabPlayer;
import com.thelab.plugin.player.PlayerState;
import com.thelab.plugin.utils.FireworkUtil;
import com.thelab.plugin.utils.MessageUtil;
import com.thelab.plugin.utils.SoundUtil;
import com.thelab.plugin.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Central game orchestrator for TheLab arenas. */
public class GameManager {

    private final TheLabPlugin plugin;
    private final ExperimentRegistry experimentRegistry;
    private final ArenaResetHandler resetHandler = new ArenaResetHandler();

    // Per-arena state
    private final Map<String, ScoreManager> scoreManagers = new HashMap<>();
    private final Map<String, ExperimentRotation> rotations = new HashMap<>();
    private final Map<String, Experiment> activeExperiments = new HashMap<>();
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<String, GameTimer> gameTimers = new HashMap<>();

    public GameManager(TheLabPlugin plugin) {
        this.plugin = plugin;
        this.experimentRegistry = new ExperimentRegistry(plugin);
    }

    // ---- Game start ----

    /** Starts the countdown for an arena. */
    public void startCountdown(Arena arena) {
        arena.setState(ArenaState.STARTING);
        int seconds = plugin.getConfigManager().getLobbyCountdownSeconds();

        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (!arena.getState().equals(ArenaState.STARTING)) {
                    cancel();
                    return;
                }
                if (arena.getPlayerCount() < arena.getMinPlayers()) {
                    cancel();
                    cancelCountdown(arena);
                    return;
                }
                if (remaining <= 0) {
                    cancel();
                    startGame(arena);
                    return;
                }
                if (remaining <= 10 || remaining % 10 == 0) {
                    String msg = plugin.getConfigManager().getMessage("game.countdown-title",
                            "seconds", String.valueOf(remaining));
                    arena.broadcastTitle("&e" + remaining, "&fGame starting!", 5, 25, 5);
                    arena.broadcast("&eGame starting in &6" + remaining + " &eseconds!");
                }
                if (remaining <= 5) {
                    for (Player p : arena.getPlayers()) SoundUtil.playCountdownTick(p);
                } else if (remaining == 1) {
                    for (Player p : arena.getPlayers()) SoundUtil.playCountdownFinal(p);
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(arena.getId(), task);
    }

    /** Cancels the countdown and returns arena to WAITING. */
    public void cancelCountdown(Arena arena) {
        BukkitTask task = countdownTasks.remove(arena.getId());
        if (task != null) task.cancel();
        arena.setState(ArenaState.WAITING);
        arena.broadcast(plugin.getConfigManager().getMessage("arena.cancelled"));
    }

    /** Starts a game in the arena (called after countdown ends). */
    public void startGame(Arena arena) {
        countdownTasks.remove(arena.getId());

        // Save region
        if (arena.getConfig().getArenaMin() != null) {
            arena.getRegionManager().saveRegion();
        }

        // Setup score manager and rotation
        ScoreManager scores = new ScoreManager();
        for (Player p : arena.getPlayers()) scores.registerPlayer(p.getUniqueId());
        scoreManagers.put(arena.getId(), scores);

        ExperimentRotation rotation = new ExperimentRotation();
        List<ExperimentType> pool = arena.getConfig().getEnabledExperiments();
        int count = plugin.getConfigManager().getExperimentsPerGame();
        rotation.selectExperiments(pool, count);
        rotations.put(arena.getId(), rotation);

        // Set all players to IN_GAME
        for (Player p : arena.getPlayers()) {
            LabPlayer lp = plugin.getPlayerManager().getLabPlayer(p);
            lp.setState(PlayerState.IN_GAME);
        }

        startNextExperiment(arena);
    }

    /** Advances to the next experiment. */
    private void startNextExperiment(Arena arena) {
        ExperimentRotation rotation = rotations.get(arena.getId());
        ScoreManager scores = scoreManagers.get(arena.getId());
        if (rotation == null || scores == null) return;

        if (!rotation.advance()) {
            endGame(arena);
            return;
        }

        ExperimentType type = rotation.getCurrentExperiment();
        int round = rotation.getRound();
        int totalRounds = rotation.getTotalRounds();

        arena.setState(ArenaState.EXPERIMENT_INTRO);
        // Reset spectators back to players for new experiment (if desired)
        // For simplicity, keep eliminated players as spectators

        // Intro phase
        arena.broadcastTitle("&e&lROUND " + round + "/" + totalRounds,
                "&a" + type.getDisplayName(), 10, 60, 10);
        arena.broadcast("&e--- Round " + round + "/" + totalRounds + " ---");
        arena.broadcast("&fExperiment: &a" + type.getDisplayName());
        arena.broadcast("&7" + type.getDescription());

        // Narrator intro
        plugin.getNarratorManager().sendExperimentIntro(arena, type);

        // Freeze players during intro
        for (Player p : arena.getPlayers()) {
            p.setWalkSpeed(0.0f);
        }

        // After intro timer, start experiment
        int introSecs = plugin.getConfigManager().getExperimentIntroSeconds();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arena.getState().equals(ArenaState.EXPERIMENT_INTRO)) return;
                // Unfreeze
                for (Player p : arena.getPlayers()) p.setWalkSpeed(0.2f);
                arena.setState(ArenaState.EXPERIMENT_PLAY);
                scores.resetExperimentScores();

                // Create and start experiment
                Experiment exp = experimentRegistry.create(type, arena, scores);
                activeExperiments.put(arena.getId(), exp);
                for (Player p : arena.getPlayers()) {
                    SoundUtil.play(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                }
                exp.start(arena.getPlayers(), () -> {
                    // Called when experiment ends
                    activeExperiments.remove(arena.getId());
                    endExperiment(arena, type);
                });
            }
        }.runTaskLater(plugin, introSecs * 20L);
    }

    /** Ends the current experiment and shows results. */
    private void endExperiment(Arena arena, ExperimentType type) {
        arena.setState(ArenaState.EXPERIMENT_RESULTS);

        ScoreManager scores = scoreManagers.get(arena.getId());
        if (scores != null) {
            scores.awardExperimentPoints(plugin.getConfigManager());
        }

        // Show results
        arena.broadcast("&e--- Experiment Results ---");
        if (scores != null) {
            ExperimentRotation rotation = rotations.get(arena.getId());
            List<UUID> ranking = scores.getExperimentRanking();
            for (int i = 0; i < Math.min(ranking.size(), 5); i++) {
                UUID uuid = ranking.get(i);
                Player p = Bukkit.getPlayer(uuid);
                String name = p != null ? p.getName() : "Unknown";
                arena.broadcast("&7" + (i + 1) + ". &f" + name + " &7- &e" + scores.getScore(uuid) + " pts");
            }
        }

        for (Player p : arena.getAllParticipants()) {
            SoundUtil.play(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Re-add spectators back to players list for next round
        List<UUID> specUUIDs = new ArrayList<>(arena.getSpectatorUUIDs());
        for (UUID uuid : specUUIDs) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                arena.removeSpectator(p);
                arena.addPlayer(p);
                p.setGameMode(GameMode.ADVENTURE);
                if (arena.getLobbySpawn() != null) p.teleport(arena.getLobbySpawn());
                if (scores != null) scores.registerPlayer(uuid);
            }
        }

        // Wait then start next experiment
        int resultSecs = plugin.getConfigManager().getExperimentResultsSeconds();
        new BukkitRunnable() {
            @Override
            public void run() {
                ExperimentRotation rotation = rotations.get(arena.getId());
                if (rotation != null && rotation.hasMore()) {
                    startNextExperiment(arena);
                } else {
                    endGame(arena);
                }
            }
        }.runTaskLater(plugin, resultSecs * 20L);
    }

    /** Ends the entire game and shows final results. */
    public void endGame(Arena arena) {
        arena.setState(ArenaState.GAME_END);

        ScoreManager scores = scoreManagers.get(arena.getId());
        WinnerCalculator calc = new WinnerCalculator();

        UUID winnerUUID = scores != null ? calc.calculateWinner(scores) : null;
        Player winner = winnerUUID != null ? Bukkit.getPlayer(winnerUUID) : null;
        String winnerName = winner != null ? winner.getName() : "Nobody";

        // Announce winner
        arena.broadcastTitle("&6&lGAME OVER!", "&6" + winnerName + " &ewins!", 10, 80, 20);
        arena.broadcast("&6&l=== GAME OVER ===");
        arena.broadcast("&6Winner: &f" + winnerName);
        arena.broadcast("&e--- Final Standings ---");

        if (scores != null) {
            List<UUID> rankings = calc.getFinalRankings(scores);
            for (int i = 0; i < rankings.size(); i++) {
                UUID uuid = rankings.get(i);
                Player p = Bukkit.getPlayer(uuid);
                String name = p != null ? p.getName() : "Unknown";
                arena.broadcast("&7" + (i + 1) + ". &f" + name + " &7- &e" + scores.getTotalScore(uuid) + " pts");
            }
        }

        // Fireworks at winner
        if (winner != null && winner.isOnline()) {
            FireworkUtil.launchCelebration(plugin, winner.getLocation());
        }

        // Update stats
        if (scores != null && plugin.getStatsManager() != null) {
            plugin.getStatsManager().recordGameEnd(arena, scores, winnerUUID);
        }

        // After delay, restore players and reset arena
        int delaySecs = plugin.getConfigManager().getGameEndDisplaySeconds();
        new BukkitRunnable() {
            @Override
            public void run() {
                resetArena(arena);
            }
        }.runTaskLater(plugin, delaySecs * 20L);
    }

    /** Resets arena and restores all players. */
    public void resetArena(Arena arena) {
        arena.setState(ArenaState.RESETTING);

        // Restore all players
        for (Player p : arena.getAllParticipants()) {
            restorePlayer(p, arena);
        }
        arena.clearAll();

        // Clean up game state
        scoreManagers.remove(arena.getId());
        rotations.remove(arena.getId());
        Experiment exp = activeExperiments.remove(arena.getId());
        if (exp != null) exp.end();

        // Reset arena region
        resetHandler.reset(arena, () -> {
            arena.setState(ArenaState.WAITING);
        });
    }

    private void restorePlayer(Player player, Arena arena) {
        LabPlayer lp = plugin.getPlayerManager().getLabPlayer(player);
        lp.setState(PlayerState.NONE);
        lp.setArena(null);
        lp.restoreInventory();

        // Teleport to main lobby
        org.bukkit.configuration.ConfigurationSection mainLobby =
                plugin.getConfigManager().getConfig().getConfigurationSection("main-lobby");
        if (mainLobby != null) {
            org.bukkit.Location loc = com.thelab.plugin.utils.LocationUtil.fromConfig(mainLobby);
            if (loc != null && loc.getWorld() != null) {
                player.teleport(loc);
            }
        }
    }

    /** Called when a player is eliminated from an experiment. */
    public void handleElimination(Arena arena, Player player) {
        LabPlayer lp = plugin.getPlayerManager().getLabPlayer(player);
        lp.setState(PlayerState.SPECTATING);
        player.setGameMode(GameMode.SPECTATOR);
        SoundUtil.playElimination(player);
        MessageUtil.sendRaw(player, "&cYou have been eliminated!");
    }

    /** Adds points to a player in an arena. */
    public void addPoints(Arena arena, Player player, int points) {
        ScoreManager scores = scoreManagers.get(arena.getId());
        if (scores != null) {
            scores.addScore(player.getUniqueId(), points);
        }
    }

    /** Gets the ScoreManager for an arena. */
    public Optional<ScoreManager> getScoreManager(Arena arena) {
        return Optional.ofNullable(scoreManagers.get(arena.getId()));
    }

    /** Gets the ExperimentRotation for an arena. */
    public Optional<ExperimentRotation> getRotation(Arena arena) {
        return Optional.ofNullable(rotations.get(arena.getId()));
    }

    /** Gets the active Experiment for an arena. */
    public Optional<Experiment> getActiveExperiment(Arena arena) {
        return Optional.ofNullable(activeExperiments.get(arena.getId()));
    }

    /** Force-starts an arena. */
    public void forceStart(Arena arena) {
        BukkitTask existing = countdownTasks.remove(arena.getId());
        if (existing != null) existing.cancel();
        startGame(arena);
    }

    /** Cleanup all active games (on plugin disable). */
    public void shutdownAll() {
        for (Map.Entry<String, Experiment> entry : activeExperiments.entrySet()) {
            try { entry.getValue().end(); } catch (Exception ignored) {}
        }
        activeExperiments.clear();
        for (BukkitTask task : countdownTasks.values()) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        countdownTasks.clear();
    }
}

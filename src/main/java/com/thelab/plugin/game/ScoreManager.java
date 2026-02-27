package com.thelab.plugin.game;

import com.thelab.plugin.config.ConfigManager;

import java.util.*;

/** Tracks player scores within a game session. */
public class ScoreManager {

    private final Map<UUID, Integer> experimentScores = new HashMap<>();
    private final Map<UUID, Integer> totalScores = new HashMap<>();

    /** Adds points to a player's current experiment score. */
    public void addScore(UUID player, int points) {
        experimentScores.merge(player, points, Integer::sum);
    }

    /** Returns a player's current experiment score. */
    public int getScore(UUID player) {
        return experimentScores.getOrDefault(player, 0);
    }

    /** Returns a player's total score across all experiments. */
    public int getTotalScore(UUID player) {
        return totalScores.getOrDefault(player, 0);
    }

    /** Adds points directly to a player's total score. */
    public void addToTotal(UUID player, int points) {
        totalScores.merge(player, points, Integer::sum);
    }

    /** Returns all tracked players. */
    public Set<UUID> getPlayers() {
        Set<UUID> all = new HashSet<>(experimentScores.keySet());
        all.addAll(totalScores.keySet());
        return all;
    }

    /** Registers a player (so they appear in rankings even with 0 score). */
    public void registerPlayer(UUID player) {
        experimentScores.putIfAbsent(player, 0);
        totalScores.putIfAbsent(player, 0);
    }

    /** Returns player UUIDs sorted by experiment score (highest first). */
    public List<UUID> getExperimentRanking() {
        List<UUID> list = new ArrayList<>(experimentScores.keySet());
        list.sort((a, b) -> experimentScores.getOrDefault(b, 0) - experimentScores.getOrDefault(a, 0));
        return list;
    }

    /** Returns player UUIDs sorted by total score (highest first). */
    public List<UUID> getTotalRanking() {
        List<UUID> list = new ArrayList<>(totalScores.keySet());
        list.sort((a, b) -> totalScores.getOrDefault(b, 0) - totalScores.getOrDefault(a, 0));
        return list;
    }

    /** Returns the 1-based rank of a player in experiment score. */
    public int getExperimentRank(UUID player) {
        List<UUID> ranking = getExperimentRanking();
        int idx = ranking.indexOf(player);
        return idx >= 0 ? idx + 1 : ranking.size() + 1;
    }

    /** Returns the 1-based rank of a player in total score. */
    public int getTotalRank(UUID player) {
        List<UUID> ranking = getTotalRanking();
        int idx = ranking.indexOf(player);
        return idx >= 0 ? idx + 1 : ranking.size() + 1;
    }

    /**
     * Awards points to top players based on experiment ranking.
     * First = configManager.getFirstPlacePoints(), etc.
     */
    public void awardExperimentPoints(ConfigManager cm) {
        List<UUID> ranking = getExperimentRanking();
        int[] awards = {
                cm.getFirstPlacePoints(),
                cm.getSecondPlacePoints(),
                cm.getThirdPlacePoints()
        };
        for (int i = 0; i < ranking.size(); i++) {
            int pts = (i < awards.length) ? awards[i] : cm.getParticipationPoints();
            addToTotal(ranking.get(i), pts);
        }
    }

    /** Resets the current experiment scores (totals are preserved). */
    public void resetExperimentScores() {
        Set<UUID> players = new HashSet<>(experimentScores.keySet());
        experimentScores.clear();
        for (UUID p : players) {
            experimentScores.put(p, 0);
        }
    }

    /** Clears all scores. */
    public void clear() {
        experimentScores.clear();
        totalScores.clear();
    }
}

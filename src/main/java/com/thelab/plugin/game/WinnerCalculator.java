package com.thelab.plugin.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** Calculates and formats game winner/standings. */
public class WinnerCalculator {

    /** Returns the UUID of the player with the highest total score. */
    public UUID calculateWinner(ScoreManager scores) {
        List<UUID> ranking = scores.getTotalRanking();
        return ranking.isEmpty() ? null : ranking.get(0);
    }

    /** Returns UUIDs sorted by total score descending. */
    public List<UUID> getFinalRankings(ScoreManager scores) {
        return scores.getTotalRanking();
    }

    /** Formats a short results string for chat. */
    public String formatResults(List<UUID> rankings, ScoreManager scores) {
        String[] medals = {"\u00a76\u00a7l1st", "\u00a7e\u00a7l2nd", "\u00a77\u00a7l3rd"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(rankings.size(), 3); i++) {
            UUID uuid = rankings.get(i);
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString().substring(0, 8);
            sb.append(medals[i]).append("\u00a7f ").append(name)
              .append(" \u00a77- \u00a7e").append(scores.getTotalScore(uuid)).append(" pts\n");
        }
        return sb.toString().trim();
    }
}

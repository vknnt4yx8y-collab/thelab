package com.thelab.plugin.stats;

import java.util.UUID;

/** Statistics for a single player. */
public class PlayerStats {

    private UUID uuid;
    private String playerName;
    private int gamesPlayed;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int pointsEarned;
    private int experimentsWon;
    private long playtimeSeconds;

    public PlayerStats(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    // Getters / setters
    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int n) { this.gamesPlayed = n; }
    public void incrementGamesPlayed() { this.gamesPlayed++; }

    public int getWins() { return wins; }
    public void setWins(int n) { this.wins = n; }
    public void incrementWins() { this.wins++; }

    public int getLosses() { return losses; }
    public void setLosses(int n) { this.losses = n; }
    public void incrementLosses() { this.losses++; }

    public int getKills() { return kills; }
    public void setKills(int n) { this.kills = n; }
    public void addKills(int n) { this.kills += n; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int n) { this.deaths = n; }
    public void addDeaths(int n) { this.deaths += n; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int n) { this.pointsEarned = n; }
    public void addPoints(int n) { this.pointsEarned += n; }

    public int getExperimentsWon() { return experimentsWon; }
    public void setExperimentsWon(int n) { this.experimentsWon = n; }
    public void incrementExperimentsWon() { this.experimentsWon++; }

    public long getPlaytimeSeconds() { return playtimeSeconds; }
    public void setPlaytimeSeconds(long n) { this.playtimeSeconds = n; }
    public void addPlaytime(long n) { this.playtimeSeconds += n; }

    public double getKdr() {
        return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
    }

    public double getWinRate() {
        return gamesPlayed == 0 ? 0.0 : Math.round((double) wins / gamesPlayed * 1000.0) / 10.0;
    }
}

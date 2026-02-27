package com.thelab.plugin.stats;

import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.game.ScoreManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/** Manages player statistics with SQLite storage. */
public class StatsManager {

    private final JavaPlugin plugin;
    private Connection connection;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();

    public StatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Initializes the database connection. */
    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "stats.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize stats database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tl_stats (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT,
                    games_played INTEGER DEFAULT 0,
                    wins INTEGER DEFAULT 0,
                    losses INTEGER DEFAULT 0,
                    kills INTEGER DEFAULT 0,
                    deaths INTEGER DEFAULT 0,
                    points_earned INTEGER DEFAULT 0,
                    experiments_won INTEGER DEFAULT 0,
                    playtime_seconds INTEGER DEFAULT 0
                )
            """);
        }
    }

    /** Loads stats for a player (async). */
    public void loadStats(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerStats stats = loadFromDb(uuid, name);
            Bukkit.getScheduler().runTask(plugin, () -> cache.put(uuid, stats));
        });
    }

    private PlayerStats loadFromDb(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM tl_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PlayerStats stats = new PlayerStats(uuid, rs.getString("player_name"));
                stats.setGamesPlayed(rs.getInt("games_played"));
                stats.setWins(rs.getInt("wins"));
                stats.setLosses(rs.getInt("losses"));
                stats.setKills(rs.getInt("kills"));
                stats.setDeaths(rs.getInt("deaths"));
                stats.setPointsEarned(rs.getInt("points_earned"));
                stats.setExperimentsWon(rs.getInt("experiments_won"));
                stats.setPlaytimeSeconds(rs.getLong("playtime_seconds"));
                return stats;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load stats for " + uuid, e);
        }
        return new PlayerStats(uuid, name);
    }

    /** Saves stats for a player (async). */
    public void saveStats(PlayerStats stats) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO tl_stats (uuid, player_name, games_played, wins, losses, kills, deaths,
                    points_earned, experiments_won, playtime_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    games_played = excluded.games_played,
                    wins = excluded.wins,
                    losses = excluded.losses,
                    kills = excluded.kills,
                    deaths = excluded.deaths,
                    points_earned = excluded.points_earned,
                    experiments_won = excluded.experiments_won,
                    playtime_seconds = excluded.playtime_seconds
            """)) {
                ps.setString(1, stats.getUuid().toString());
                ps.setString(2, stats.getPlayerName());
                ps.setInt(3, stats.getGamesPlayed());
                ps.setInt(4, stats.getWins());
                ps.setInt(5, stats.getLosses());
                ps.setInt(6, stats.getKills());
                ps.setInt(7, stats.getDeaths());
                ps.setInt(8, stats.getPointsEarned());
                ps.setInt(9, stats.getExperimentsWon());
                ps.setLong(10, stats.getPlaytimeSeconds());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save stats for " + stats.getUuid(), e);
            }
        });
    }

    /** Gets cached stats for a player. */
    public PlayerStats getStats(UUID uuid) {
        return cache.getOrDefault(uuid, new PlayerStats(uuid, "Unknown"));
    }

    /** Records game end, updating stats for all players. */
    public void recordGameEnd(Arena arena, ScoreManager scores, UUID winnerUUID) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (UUID uuid : scores.getPlayers()) {
                PlayerStats stats = cache.computeIfAbsent(uuid,
                        u -> new PlayerStats(u, Optional.ofNullable(Bukkit.getOfflinePlayer(u).getName()).orElse("Unknown")));
                stats.incrementGamesPlayed();
                stats.addPoints(scores.getTotalScore(uuid));
                if (uuid.equals(winnerUUID)) {
                    stats.incrementWins();
                } else {
                    stats.incrementLosses();
                }
                saveStats(stats);
            }
        });
    }

    /** Returns top N players by wins. */
    public List<PlayerStats> getLeaderboard(String category, int limit) {
        List<PlayerStats> list = new ArrayList<>(cache.values());
        Comparator<PlayerStats> comp = switch (category.toLowerCase()) {
            case "wins" -> Comparator.comparingInt(PlayerStats::getWins).reversed();
            case "kills" -> Comparator.comparingInt(PlayerStats::getKills).reversed();
            case "points" -> Comparator.comparingInt(PlayerStats::getPointsEarned).reversed();
            default -> Comparator.comparingInt(PlayerStats::getGamesPlayed).reversed();
        };
        list.sort(comp);
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** Closes the database connection. */
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}

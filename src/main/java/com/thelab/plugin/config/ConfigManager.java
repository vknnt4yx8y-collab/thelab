package com.thelab.plugin.config;

import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** Manages all plugin configuration files. */
public class ConfigManager {

    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration scoreboard;
    private FileConfiguration sounds;
    private FileConfiguration experiments;
    private FileConfiguration arenas;

    private File configFile, messagesFile, scoreboardFile, soundsFile, experimentsFile, arenasFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads (or reloads) all configuration files from disk. */
    public void load() {
        plugin.saveDefaultConfig();
        saveDefault("messages.yml");
        saveDefault("scoreboard.yml");
        saveDefault("sounds.yml");
        saveDefault("experiments.yml");
        saveDefault("arenas.yml");

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        scoreboardFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        soundsFile = new File(plugin.getDataFolder(), "sounds.yml");
        experimentsFile = new File(plugin.getDataFolder(), "experiments.yml");
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");

        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        scoreboard = YamlConfiguration.loadConfiguration(scoreboardFile);
        sounds = YamlConfiguration.loadConfiguration(soundsFile);
        experiments = YamlConfiguration.loadConfiguration(experimentsFile);
        arenas = YamlConfiguration.loadConfiguration(arenasFile);
    }

    private void saveDefault(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) {
            plugin.saveResource(name, false);
        }
    }

    /** Saves the arenas config to disk. */
    public void saveArenas() {
        try {
            arenas.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save arenas.yml", e);
        }
    }

    // ---- Config getters ----

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getScoreboard() { return scoreboard; }
    public FileConfiguration getSounds() { return sounds; }
    public FileConfiguration getExperiments() { return experiments; }
    public FileConfiguration getArenas() { return arenas; }

    public String getPrefix() {
        return MessageUtil.colorize(messages.getString("prefix", "&8[&aTheLab&8] &r"));
    }

    /**
     * Gets a message from messages.yml, applies color codes, and replaces
     * {key} placeholders with provided key-value pairs.
     *
     * @param path     dot-separated path in messages.yml
     * @param replacements alternating key, value pairs (e.g. "player", "Steve")
     * @return colored, replaced message string
     */
    public String getMessage(String path, String... replacements) {
        String raw = messages.getString(path, "&cMissing message: " + path);
        raw = raw.replace("{prefix}", getPrefix());
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return MessageUtil.colorize(raw);
    }

    /**
     * Gets a list of narrator messages for a given event key.
     * e.g. event = "experiment-intro.dodge-ball" maps to narrator.experiment-intro.dodge-ball
     */
    public List<String> getNarratorMessages(String event) {
        List<String> raw = messages.getStringList("narrator." + event);
        List<String> result = new ArrayList<>();
        for (String s : raw) {
            result.add(MessageUtil.colorize(s));
        }
        if (result.isEmpty()) {
            result.add(MessageUtil.colorize("&aWelcome to the experiment!"));
        }
        return result;
    }

    // ---- Game settings ----

    public int getExperimentsPerGame() { return config.getInt("game.experiments-per-game", 3); }
    public int getMinPlayers() { return config.getInt("game.min-players", 2); }
    public int getMaxPlayers() { return config.getInt("game.max-players", 16); }
    public int getLobbyCountdownSeconds() { return config.getInt("game.lobby-countdown-seconds", 30); }
    public int getExperimentIntroSeconds() { return config.getInt("game.experiment-intro-seconds", 5); }
    public int getExperimentResultsSeconds() { return config.getInt("game.experiment-results-seconds", 5); }
    public int getGameEndDisplaySeconds() { return config.getInt("game.game-end-display-seconds", 10); }
    public boolean isArenaChatOnly() { return config.getBoolean("game.arena-chat-only", true); }
    public boolean allowSpectators() { return config.getBoolean("game.allow-spectators", true); }

    // ---- Scoring ----

    public int getFirstPlacePoints() { return config.getInt("game.scoring.first-place", 3); }
    public int getSecondPlacePoints() { return config.getInt("game.scoring.second-place", 2); }
    public int getThirdPlacePoints() { return config.getInt("game.scoring.third-place", 1); }
    public int getParticipationPoints() { return config.getInt("game.scoring.participation", 0); }

    // ---- Database ----

    public String getDatabaseType() { return config.getString("database.type", "SQLITE"); }
    public String getSqliteFile() { return config.getString("database.sqlite.file", "stats.db"); }
    public String getMysqlHost() { return config.getString("database.mysql.host", "localhost"); }
    public int getMysqlPort() { return config.getInt("database.mysql.port", 3306); }
    public String getMysqlDatabase() { return config.getString("database.mysql.database", "thelab"); }
    public String getMysqlUsername() { return config.getString("database.mysql.username", "root"); }
    public String getMysqlPassword() { return config.getString("database.mysql.password", ""); }
    public String getMysqlTablePrefix() { return config.getString("database.mysql.table-prefix", "tl_"); }
    public int getMysqlPoolSize() { return config.getInt("database.mysql.pool-size", 10); }

    // ---- Bungee ----

    public boolean isBungeeEnabled() { return config.getBoolean("bungee.enabled", false); }
    public String getBungeeMode() { return config.getString("bungee.mode", "STANDALONE"); }

    // ---- Scoreboard ----

    public boolean isScoreboardEnabled() { return config.getBoolean("scoreboard.enabled", true); }
    public int getScoreboardUpdateInterval() { return config.getInt("scoreboard.update-interval", 10); }
}

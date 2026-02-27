package com.thelab.plugin.experiment;

import com.thelab.plugin.arena.Arena;
import org.bukkit.configuration.ConfigurationSection;

/** Configuration for a specific experiment. */
public class ExperimentConfig {

    private final ExperimentType type;
    private final ConfigurationSection section;

    public ExperimentConfig(ExperimentType type, ConfigurationSection section) {
        this.type = type;
        this.section = section;
    }

    public ExperimentType getType() { return type; }

    public int getDuration() {
        return section != null ? section.getInt("duration", 120) : 120;
    }

    public int getMinPlayers() {
        return section != null ? section.getInt("min-players", 2) : 2;
    }

    public int getInt(String key, int def) {
        return section != null ? section.getInt(key, def) : def;
    }

    public double getDouble(String key, double def) {
        return section != null ? section.getDouble(key, def) : def;
    }

    public boolean getBoolean(String key, boolean def) {
        return section != null ? section.getBoolean(key, def) : def;
    }

    public String getString(String key, String def) {
        return section != null ? section.getString(key, def) : def;
    }
}

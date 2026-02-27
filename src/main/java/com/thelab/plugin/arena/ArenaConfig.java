package com.thelab.plugin.arena;

import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;

/** Configuration data for a TheLab arena. */
public class ArenaConfig {

    private String id;
    private String displayName;
    private int minPlayers;
    private int maxPlayers;
    private boolean enabled;
    private Location lobbySpawn;
    private Location spectatorSpawn;
    private Location arenaMin;
    private Location arenaMax;
    private List<ExperimentType> enabledExperiments = new ArrayList<>();
    private Map<ExperimentType, List<Location>> experimentSpawns = new EnumMap<>(ExperimentType.class);

    public ArenaConfig(String id) {
        this.id = id;
        this.displayName = id;
        this.minPlayers = 2;
        this.maxPlayers = 16;
        this.enabled = false;
    }

    // ---- Getters / Setters ----

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int n) { this.minPlayers = n; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int n) { this.maxPlayers = n; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location loc) { this.lobbySpawn = loc; }

    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location loc) { this.spectatorSpawn = loc; }

    public Location getArenaMin() { return arenaMin; }
    public void setArenaMin(Location loc) { this.arenaMin = loc; }

    public Location getArenaMax() { return arenaMax; }
    public void setArenaMax(Location loc) { this.arenaMax = loc; }

    public List<ExperimentType> getEnabledExperiments() { return enabledExperiments; }
    public void setEnabledExperiments(List<ExperimentType> list) { this.enabledExperiments = list; }

    public void addExperiment(ExperimentType type) {
        if (!enabledExperiments.contains(type)) enabledExperiments.add(type);
    }

    public void removeExperiment(ExperimentType type) {
        enabledExperiments.remove(type);
        experimentSpawns.remove(type);
    }

    public Map<ExperimentType, List<Location>> getExperimentSpawns() { return experimentSpawns; }

    public List<Location> getSpawnsFor(ExperimentType type) {
        return experimentSpawns.getOrDefault(type, Collections.emptyList());
    }

    public void addSpawnFor(ExperimentType type, Location loc) {
        experimentSpawns.computeIfAbsent(type, k -> new ArrayList<>()).add(loc);
    }

    public void clearSpawnsFor(ExperimentType type) {
        experimentSpawns.remove(type);
    }

    /** Checks whether this arena has the minimum config to be used. */
    public boolean isFullySetup() {
        return lobbySpawn != null && !enabledExperiments.isEmpty();
    }

    // ---- Serialization ----

    /** Serializes this config to a Map for YAML storage. */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("display-name", displayName);
        map.put("min-players", minPlayers);
        map.put("max-players", maxPlayers);
        map.put("enabled", enabled);
        if (lobbySpawn != null) map.put("lobby-spawn", LocationUtil.serialize(lobbySpawn));
        if (spectatorSpawn != null) map.put("spectator-spawn", LocationUtil.serialize(spectatorSpawn));
        if (arenaMin != null) map.put("arena-min", LocationUtil.serialize(arenaMin));
        if (arenaMax != null) map.put("arena-max", LocationUtil.serialize(arenaMax));

        List<String> expList = new ArrayList<>();
        for (ExperimentType t : enabledExperiments) expList.add(t.getConfigKey());
        map.put("experiments", expList);

        Map<String, Object> spawnMap = new LinkedHashMap<>();
        for (Map.Entry<ExperimentType, List<Location>> entry : experimentSpawns.entrySet()) {
            List<Map<String, Object>> locs = new ArrayList<>();
            for (Location loc : entry.getValue()) locs.add(LocationUtil.serialize(loc));
            spawnMap.put(entry.getKey().getConfigKey(), locs);
        }
        map.put("experiment-spawns", spawnMap);
        return map;
    }

    /** Deserializes an ArenaConfig from a ConfigurationSection in arenas.yml. */
    @SuppressWarnings("unchecked")
    public static ArenaConfig deserialize(String id, ConfigurationSection sec) {
        ArenaConfig cfg = new ArenaConfig(id);
        if (sec == null) return cfg;
        cfg.setDisplayName(sec.getString("display-name", id));
        cfg.setMinPlayers(sec.getInt("min-players", 2));
        cfg.setMaxPlayers(sec.getInt("max-players", 16));
        cfg.setEnabled(sec.getBoolean("enabled", false));

        ConfigurationSection lobby = sec.getConfigurationSection("lobby-spawn");
        if (lobby != null) cfg.setLobbySpawn(LocationUtil.fromConfig(lobby));

        ConfigurationSection spec = sec.getConfigurationSection("spectator-spawn");
        if (spec != null) cfg.setSpectatorSpawn(LocationUtil.fromConfig(spec));

        ConfigurationSection min = sec.getConfigurationSection("arena-min");
        if (min != null) cfg.setArenaMin(LocationUtil.fromConfig(min));

        ConfigurationSection max = sec.getConfigurationSection("arena-max");
        if (max != null) cfg.setArenaMax(LocationUtil.fromConfig(max));

        List<String> expKeys = sec.getStringList("experiments");
        for (String key : expKeys) {
            ExperimentType t = ExperimentType.fromConfigKey(key);
            if (t != null) cfg.addExperiment(t);
        }

        ConfigurationSection spawnSec = sec.getConfigurationSection("experiment-spawns");
        if (spawnSec != null) {
            for (String key : spawnSec.getKeys(false)) {
                ExperimentType t = ExperimentType.fromConfigKey(key);
                if (t == null) continue;
                List<?> locList = spawnSec.getList(key);
                if (locList == null) continue;
                for (Object obj : locList) {
                    if (obj instanceof Map<?, ?> m) {
                        Location loc = LocationUtil.deserialize((Map<?, ?>) m);
                        if (loc != null) cfg.addSpawnFor(t, loc);
                    }
                }
            }
        }
        return cfg;
    }
}

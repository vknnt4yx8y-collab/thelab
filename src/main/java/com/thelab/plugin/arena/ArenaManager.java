package com.thelab.plugin.arena;

import com.thelab.plugin.config.ConfigManager;
import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/** Manages all arena instances. */
public class ArenaManager {

    private final ConfigManager configManager;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();

    public ArenaManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /** Loads all arenas from arenas.yml. */
    public void loadArenas() {
        arenas.clear();
        FileConfiguration cfg = configManager.getArenas();
        ConfigurationSection arenasSection = cfg.getConfigurationSection("arenas");
        if (arenasSection == null) return;
        for (String id : arenasSection.getKeys(false)) {
            ConfigurationSection sec = arenasSection.getConfigurationSection(id);
            ArenaConfig config = ArenaConfig.deserialize(id, sec);
            arenas.put(id.toLowerCase(), new Arena(config));
        }
    }

    /** Saves all arenas to arenas.yml. */
    public void saveArenas() {
        FileConfiguration cfg = configManager.getArenas();
        cfg.set("arenas", null);
        for (Arena arena : arenas.values()) {
            cfg.createSection("arenas." + arena.getId(), arena.getConfig().serialize());
        }
        configManager.saveArenas();
    }

    /** Creates a new arena with the given ID. */
    public Arena createArena(String id, int minPlayers, int maxPlayers) {
        String key = id.toLowerCase();
        ArenaConfig config = new ArenaConfig(key);
        config.setDisplayName(id);
        config.setMinPlayers(minPlayers);
        config.setMaxPlayers(maxPlayers);
        Arena arena = new Arena(config);
        arenas.put(key, arena);
        saveArenas();
        return arena;
    }

    /** Deletes an arena by ID. Returns true if found and removed. */
    public boolean deleteArena(String id) {
        Arena removed = arenas.remove(id.toLowerCase());
        if (removed != null) {
            // Remove all players from the arena first
            for (Player p : removed.getPlayers()) {
                removed.removePlayer(p);
            }
            saveArenas();
            return true;
        }
        return false;
    }

    /** Gets an arena by ID. */
    public Optional<Arena> getArena(String id) {
        return Optional.ofNullable(arenas.get(id.toLowerCase()));
    }

    /** Returns all arenas. */
    public Collection<Arena> getArenas() { return Collections.unmodifiableCollection(arenas.values()); }

    /** Finds which arena a player is currently in. */
    public Optional<Arena> getArenaForPlayer(UUID uuid) {
        for (Arena arena : arenas.values()) {
            if (arena.getPlayerUUIDs().contains(uuid) || arena.getSpectatorUUIDs().contains(uuid)) {
                return Optional.of(arena);
            }
        }
        return Optional.empty();
    }

    /** Finds which arena a player is currently in. */
    public Optional<Arena> getArenaForPlayer(Player player) {
        return getArenaForPlayer(player.getUniqueId());
    }

    /** Returns the best joinable arena (most players, not full). */
    public Optional<Arena> getJoinableArena() {
        return arenas.values().stream()
                .filter(Arena::isJoinable)
                .max(Comparator.comparingInt(Arena::getPlayerCount));
    }

    /** Enables an arena. */
    public boolean enableArena(String id) {
        Optional<Arena> opt = getArena(id);
        if (opt.isEmpty()) return false;
        Arena arena = opt.get();
        arena.getConfig().setEnabled(true);
        arena.setState(ArenaState.WAITING);
        saveArenas();
        return true;
    }

    /** Disables an arena. */
    public boolean disableArena(String id) {
        Optional<Arena> opt = getArena(id);
        if (opt.isEmpty()) return false;
        Arena arena = opt.get();
        arena.getConfig().setEnabled(false);
        arena.setState(ArenaState.DISABLED);
        saveArenas();
        return true;
    }

    public boolean arenaExists(String id) { return arenas.containsKey(id.toLowerCase()); }
}

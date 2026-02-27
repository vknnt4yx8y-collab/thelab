package com.thelab.plugin.sign;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaManager;
import com.thelab.plugin.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/** Manages TheLab join signs. */
public class SignManager {

    private final TheLabPlugin plugin;
    private final Map<Location, String> signs = new HashMap<>(); // location -> arena id
    private BukkitTask updateTask;
    private File signsFile;

    public SignManager(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    /** Loads signs from disk and starts update task. */
    public void load() {
        signsFile = new File(plugin.getDataFolder(), "signs.yml");
        if (signsFile.exists()) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(signsFile);
            List<?> list = cfg.getList("signs");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof Map<?, ?> map) {
                        try {
                            Location loc = LocationUtil_fromMap(map);
                            String arenaId = (String) map.get("arena");
                            if (loc != null && arenaId != null) {
                                signs.put(loc, arenaId);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        startUpdateTask();
    }

    @SuppressWarnings("unchecked")
    private Location LocationUtil_fromMap(Map<?, ?> map) {
        return com.thelab.plugin.utils.LocationUtil.deserialize((Map<Object, Object>) (Map<?, ?>) map);
    }

    /** Saves signs to disk. */
    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Location, String> entry : signs.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>(com.thelab.plugin.utils.LocationUtil.serialize(entry.getKey()));
            m.put("arena", entry.getValue());
            list.add(m);
        }
        cfg.set("signs", list);
        try { cfg.save(signsFile); }
        catch (IOException e) { plugin.getLogger().log(Level.WARNING, "Failed to save signs", e); }
    }

    /** Adds a sign. */
    public void addSign(Location location, String arenaId) {
        signs.put(location, arenaId);
        save();
        updateSign(location, arenaId);
    }

    /** Removes a sign. */
    public void removeSign(Location location) {
        signs.remove(location);
        save();
    }

    /** Returns the arena ID for a sign location, or null. */
    public String getArenaId(Location location) {
        return signs.get(location);
    }

    /** Returns whether a location is a registered sign. */
    public boolean isSign(Location location) {
        return signs.containsKey(location);
    }

    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllSigns();
            }
        }.runTaskTimer(plugin, 40L, 40L); // update every 2 seconds
    }

    private void updateAllSigns() {
        for (Map.Entry<Location, String> entry : signs.entrySet()) {
            updateSign(entry.getKey(), entry.getValue());
        }
    }

    private void updateSign(Location loc, String arenaId) {
        if (loc.getWorld() == null) return;
        org.bukkit.block.Block block = loc.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        Optional<Arena> opt = plugin.getArenaManager().getArena(arenaId);

        Component line1 = comp(opt.map(Arena::getDisplayName).orElse(arenaId));
        Component line2 = opt.map(a -> comp(a.getPlayerCount() + "/" + a.getMaxPlayers())).orElse(comp("N/A"));
        Component line3 = opt.map(a -> comp(a.getState().getColoredName())).orElse(comp("Unknown"));
        Component line4 = comp("");

        var side = sign.getSide(Side.FRONT);
        side.line(0, line1);
        side.line(1, line2);
        side.line(2, line3);
        side.line(3, line4);
        sign.update(false, false);
    }

    private Component comp(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(
                MessageUtil.colorize(s));
    }

    /** Shuts down the sign manager. */
    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
    }
}

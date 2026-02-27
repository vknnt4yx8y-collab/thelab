package com.thelab.plugin.arena;

import com.thelab.plugin.TheLabPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Saves and restores the block state of an arena region. */
public class ArenaRegionManager {

    private static final int BLOCKS_PER_TICK = 5000;

    private final ArenaConfig config;
    private final Map<BlockVector, BlockData> savedBlocks = new HashMap<>();

    public ArenaRegionManager(ArenaConfig config) {
        this.config = config;
    }

    /** Saves all blocks in the arena region to memory. Also removes non-player entities. */
    public void saveRegion() {
        Location min = config.getArenaMin();
        Location max = config.getArenaMax();
        if (min == null || max == null || min.getWorld() == null) return;

        savedBlocks.clear();

        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = min.getWorld().getBlockAt(x, y, z);
                    savedBlocks.put(new BlockVector(x, y, z), block.getBlockData().clone());
                }
            }
        }
    }

    /** Restores the arena region from the saved state, batched to avoid lag. */
    public void restoreRegion(Runnable onComplete) {
        Location min = config.getArenaMin();
        Location max = config.getArenaMax();
        if (min == null || max == null || min.getWorld() == null || savedBlocks.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Remove non-player entities in region first
        removeEntitiesInRegion(min, max);

        List<Map.Entry<BlockVector, BlockData>> entries = new ArrayList<>(savedBlocks.entrySet());

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int end = Math.min(index + BLOCKS_PER_TICK, entries.size());
                for (int i = index; i < end; i++) {
                    Map.Entry<BlockVector, BlockData> entry = entries.get(i);
                    BlockVector bv = entry.getKey();
                    Block block = min.getWorld().getBlockAt(bv.getBlockX(), bv.getBlockY(), bv.getBlockZ());
                    block.setBlockData(entry.getValue(), false);
                }
                index = end;
                if (index >= entries.size()) {
                    cancel();
                    if (onComplete != null) {
                        Bukkit.getScheduler().runTask(TheLabPlugin.getInstance(), onComplete);
                    }
                }
            }
        }.runTaskTimer(TheLabPlugin.getInstance(), 0L, 1L);
    }

    /** Removes all non-player entities within the given region. */
    private void removeEntitiesInRegion(Location min, Location max) {
        if (min.getWorld() == null) return;
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());

        for (Entity entity : min.getWorld().getEntities()) {
            if (entity instanceof Player) continue;
            Location loc = entity.getLocation();
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                    && loc.getBlockY() >= minY && loc.getBlockY() <= maxY
                    && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                entity.remove();
            }
        }
    }

    public boolean hasSavedState() { return !savedBlocks.isEmpty(); }
}

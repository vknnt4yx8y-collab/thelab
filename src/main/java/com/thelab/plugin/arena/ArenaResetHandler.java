package com.thelab.plugin.arena;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

/** Handles resetting an arena after a game. */
public class ArenaResetHandler {

    /** Resets the arena: removes items/mobs, restores region, then calls the callback. */
    public void reset(Arena arena, Runnable callback) {
        // Remove dropped items and non-player entities
        Location min = arena.getConfig().getArenaMin();
        Location max = arena.getConfig().getArenaMax();

        if (min != null && max != null && min.getWorld() != null) {
            for (Entity entity : min.getWorld().getEntities()) {
                if (entity instanceof Player) continue;
                Location loc = entity.getLocation();
                if (isInRegion(loc, min, max)) {
                    entity.remove();
                }
            }
        }

        // Restore region blocks
        arena.getRegionManager().restoreRegion(() -> {
            arena.setState(ArenaState.WAITING);
            if (callback != null) callback.run();
        });
    }

    private boolean isInRegion(Location loc, Location min, Location max) {
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                && loc.getBlockY() >= minY && loc.getBlockY() <= maxY
                && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }
}

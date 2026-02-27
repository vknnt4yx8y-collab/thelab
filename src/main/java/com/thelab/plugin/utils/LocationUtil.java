package com.thelab.plugin.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Utilities for serializing and working with Locations. */
public final class LocationUtil {

    private LocationUtil() {}

    /** Serializes a Location to a Map. */
    public static Map<String, Object> serialize(Location loc) {
        Map<String, Object> map = new HashMap<>();
        if (loc == null) return map;
        map.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", (double) loc.getYaw());
        map.put("pitch", (double) loc.getPitch());
        return map;
    }

    /** Deserializes a Location from a Map. */
    public static Location deserialize(Map<?, ?> map) {
        if (map == null) return null;
        String worldName = (String) map.get("world");
        World world = org.bukkit.Bukkit.getWorld(worldName != null ? worldName : "world");
        double x = toDouble(map.get("x"));
        double y = toDouble(map.get("y"));
        double z = toDouble(map.get("z"));
        float yaw = (float) toDouble(map.get("yaw"));
        float pitch = (float) toDouble(map.get("pitch"));
        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Reads a Location from a ConfigurationSection. */
    public static Location fromConfig(ConfigurationSection sec) {
        if (sec == null) return null;
        String worldName = sec.getString("world", "world");
        World world = org.bukkit.Bukkit.getWorld(worldName);
        double x = sec.getDouble("x", 0);
        double y = sec.getDouble("y", 64);
        double z = sec.getDouble("z", 0);
        float yaw = (float) sec.getDouble("yaw", 0);
        float pitch = (float) sec.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    /** Writes a Location to a ConfigurationSection. */
    public static void toConfig(Location loc, ConfigurationSection sec) {
        if (loc == null || sec == null) return;
        sec.set("world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", (double) loc.getYaw());
        sec.set("pitch", (double) loc.getPitch());
    }

    /** Centers the given location to the middle of its block (X and Z). */
    public static Location center(Location loc) {
        if (loc == null) return null;
        return new Location(loc.getWorld(),
                loc.getBlockX() + 0.5,
                loc.getY(),
                loc.getBlockZ() + 0.5,
                loc.getYaw(),
                loc.getPitch());
    }

    /** Returns a random Location within a cuboid region defined by two corners. */
    public static Location randomInRegion(Location min, Location max) {
        if (min == null || max == null) return min;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double minX = Math.min(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxX = Math.max(min.getX(), max.getX());
        double maxY = Math.max(min.getY(), max.getY());
        double maxZ = Math.max(min.getZ(), max.getZ());
        double x = minX + r.nextDouble() * (maxX - minX);
        double y = minY + r.nextDouble() * (maxY - minY);
        double z = minZ + r.nextDouble() * (maxZ - minZ);
        return new Location(min.getWorld(), x, y, z);
    }

    private static double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}

package com.thelab.plugin.utils;

/** Utility for formatting time. */
public final class TimeUtil {

    private TimeUtil() {}

    /** Formats seconds as MM:SS. */
    public static String formatTime(int seconds) {
        if (seconds < 0) seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    /** Formats seconds as a short human-readable string (e.g. "45s" or "1m 30s"). */
    public static String formatTimeShort(int seconds) {
        if (seconds < 0) seconds = 0;
        if (seconds < 60) return seconds + "s";
        int m = seconds / 60;
        int s = seconds % 60;
        return s == 0 ? m + "m" : m + "m " + s + "s";
    }
}

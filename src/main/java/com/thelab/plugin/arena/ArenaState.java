package com.thelab.plugin.arena;

import org.bukkit.ChatColor;

/** Represents the state of a TheLab arena. */
public enum ArenaState {
    DISABLED("Disabled", ChatColor.RED),
    WAITING("Waiting", ChatColor.GREEN),
    STARTING("Starting", ChatColor.YELLOW),
    EXPERIMENT_INTRO("Starting Round", ChatColor.AQUA),
    EXPERIMENT_PLAY("In Game", ChatColor.RED),
    EXPERIMENT_RESULTS("Results", ChatColor.GOLD),
    GAME_END("Game End", ChatColor.GOLD),
    RESETTING("Resetting", ChatColor.GRAY);

    private final String displayName;
    private final ChatColor color;

    ArenaState(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public ChatColor getColor() { return color; }

    public String getColoredName() { return color + displayName; }
}

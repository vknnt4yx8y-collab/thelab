package com.thelab.plugin.api.events;

import com.thelab.plugin.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player earns points in an experiment. */
public class PlayerScoreEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Arena arena;
    private int points;

    public PlayerScoreEvent(Player player, Arena arena, int points) {
        this.player = player; this.arena = arena; this.points = points;
    }
    public Player getPlayer() { return player; }
    public Arena getArena() { return arena; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

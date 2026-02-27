package com.thelab.plugin.api.events;

import com.thelab.plugin.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player leaves an arena. */
public class PlayerLeaveArenaEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Arena arena;

    public PlayerLeaveArenaEvent(Player player, Arena arena) {
        this.player = player; this.arena = arena;
    }
    public Player getPlayer() { return player; }
    public Arena getArena() { return arena; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

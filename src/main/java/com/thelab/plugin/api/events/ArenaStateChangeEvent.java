package com.thelab.plugin.api.events;

import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when an arena changes state. */
public class ArenaStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final ArenaState oldState;
    private final ArenaState newState;

    public ArenaStateChangeEvent(Arena arena, ArenaState oldState, ArenaState newState) {
        this.arena = arena; this.oldState = oldState; this.newState = newState;
    }
    public Arena getArena() { return arena; }
    public ArenaState getOldState() { return oldState; }
    public ArenaState getNewState() { return newState; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

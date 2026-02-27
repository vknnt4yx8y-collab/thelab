package com.thelab.plugin.api.events;

import com.thelab.plugin.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player joins an arena. Cancellable. */
public class PlayerJoinArenaEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Arena arena;
    private boolean cancelled;

    public PlayerJoinArenaEvent(Player player, Arena arena) {
        this.player = player; this.arena = arena;
    }
    public Player getPlayer() { return player; }
    public Arena getArena() { return arena; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

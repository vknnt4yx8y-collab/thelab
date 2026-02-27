package com.thelab.plugin.api.events;

import com.thelab.plugin.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a game ends in an arena. */
public class GameEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final Player winner;

    public GameEndEvent(Arena arena, Player winner) {
        this.arena = arena; this.winner = winner;
    }
    public Arena getArena() { return arena; }
    public Player getWinner() { return winner; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

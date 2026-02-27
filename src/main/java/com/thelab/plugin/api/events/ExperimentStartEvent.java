package com.thelab.plugin.api.events;

import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.ExperimentType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when an experiment starts. */
public class ExperimentStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final ExperimentType type;

    public ExperimentStartEvent(Arena arena, ExperimentType type) {
        this.arena = arena; this.type = type;
    }
    public Arena getArena() { return arena; }
    public ExperimentType getExperimentType() { return type; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

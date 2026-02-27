package com.thelab.plugin.game;

/** The phase of the game, mirroring ArenaState for game logic. */
public enum GamePhase {
    WAITING,
    STARTING,
    EXPERIMENT_INTRO,
    EXPERIMENT_PLAY,
    EXPERIMENT_RESULTS,
    GAME_END,
    RESETTING
}

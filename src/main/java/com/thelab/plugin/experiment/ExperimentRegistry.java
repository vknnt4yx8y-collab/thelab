package com.thelab.plugin.experiment;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.impl.*;
import com.thelab.plugin.game.ScoreManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/** Registry that creates Experiment instances by type. */
public class ExperimentRegistry {

    private final TheLabPlugin plugin;

    public ExperimentRegistry(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates an Experiment instance for the given type.
     *
     * @param type         the experiment type
     * @param arena        the arena to run in
     * @param scoreManager the score manager for this game
     * @return a new Experiment instance ready to be started
     */
    public Experiment create(ExperimentType type, Arena arena, ScoreManager scoreManager) {
        ConfigurationSection sec = plugin.getConfigManager().getExperiments()
                .getConfigurationSection(type.getConfigKey());
        ExperimentConfig cfg = new ExperimentConfig(type, sec);

        return switch (type) {
            case DODGE_BALL -> new DodgeBallExperiment(arena, cfg, scoreManager);
            case ELECTRIC_FLOOR -> new ElectricFloorExperiment(arena, cfg, scoreManager);
            case GOLD_RUSH -> new GoldRushExperiment(arena, cfg, scoreManager);
            case CRAZY_PAINTS -> new CrazyPaintsExperiment(arena, cfg, scoreManager);
            case BALLOON_POP -> new BalloonPopExperiment(arena, cfg, scoreManager);
            case SNOWMAN -> new SnowmanExperiment(arena, cfg, scoreManager);
            case SPLEGG -> new SpleggExperiment(arena, cfg, scoreManager);
            case FIGHT -> new FightExperiment(arena, cfg, scoreManager);
            case WHACK_A_MOB -> new WhackAMobExperiment(arena, cfg, scoreManager);
            case BOAT_WARS -> new BoatWarsExperiment(arena, cfg, scoreManager);
            case PIG_RACING -> new PigRacingExperiment(arena, cfg, scoreManager);
            case ROCKET_RACE -> new RocketRaceExperiment(arena, cfg, scoreManager);
            case BREAKING_BLOCKS -> new BreakingBlocksExperiment(arena, cfg, scoreManager);
            case CATASTROPHIC -> new CatastrophicExperiment(arena, cfg, scoreManager);
        };
    }
}

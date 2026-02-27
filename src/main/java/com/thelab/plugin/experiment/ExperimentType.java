package com.thelab.plugin.experiment;

/** All 14 experiment types supported by TheLab. */
public enum ExperimentType {
    DODGE_BALL("Dodge Ball", "dodge-ball", "Dodge snowballs to survive!"),
    ELECTRIC_FLOOR("Electric Floor", "electric-floor", "Stay on the announced color!"),
    GOLD_RUSH("Gold Rush", "gold-rush", "Collect the most gold!"),
    CRAZY_PAINTS("Crazy Paints", "crazy-paints", "Paint the most floor tiles!"),
    BALLOON_POP("Balloon Pop", "balloon-pop", "Pop balloons for points!"),
    SNOWMAN("Snowman", "snowman", "Freeze opponents with snowballs!"),
    SPLEGG("Splegg", "splegg", "Destroy the floor under opponents!"),
    FIGHT("Fight", "fight", "Kill the most enemies!"),
    WHACK_A_MOB("Whack-A-Mob", "whack-a-mob", "Hit the right mobs for points!"),
    BOAT_WARS("Boat Wars", "boat-wars", "Sink enemy boats!"),
    PIG_RACING("Pig Racing", "pig-racing", "Race your pig to victory!"),
    ROCKET_RACE("Rocket Race", "rocket-race", "Fly through all checkpoints!"),
    BREAKING_BLOCKS("Breaking Blocks", "breaking-blocks", "Mine blocks for points!"),
    CATASTROPHIC("Catastrophic", "catastrophic", "Survive the escalating disasters!");

    private final String displayName;
    private final String configKey;
    private final String description;

    ExperimentType(String displayName, String configKey, String description) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getConfigKey() { return configKey; }
    public String getDescription() { return description; }

    public static ExperimentType fromConfigKey(String key) {
        for (ExperimentType t : values()) {
            if (t.configKey.equalsIgnoreCase(key)) return t;
        }
        return null;
    }
}

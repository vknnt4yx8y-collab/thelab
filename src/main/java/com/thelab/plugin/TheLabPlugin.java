package com.thelab.plugin;

import com.thelab.plugin.arena.ArenaJoinHandler;
import com.thelab.plugin.arena.ArenaManager;
import com.thelab.plugin.arena.ArenaSetupWizard;
import com.thelab.plugin.commands.TheLabCommand;
import com.thelab.plugin.config.ConfigManager;
import com.thelab.plugin.game.GameManager;
import com.thelab.plugin.listeners.ArenaProtectionListener;
import com.thelab.plugin.listeners.ChatListener;
import com.thelab.plugin.listeners.PlayerConnectionListener;
import com.thelab.plugin.listeners.PlayerProtectionListener;
import com.thelab.plugin.listeners.WorldProtectionListener;
import com.thelab.plugin.narrator.DrZuk;
import com.thelab.plugin.player.PlayerManager;
import com.thelab.plugin.scoreboard.ScoreboardManager;
import com.thelab.plugin.sign.SignListener;
import com.thelab.plugin.sign.SignManager;
import com.thelab.plugin.stats.StatsManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/** Main plugin class for TheLab MiniGame. */
public class TheLabPlugin extends JavaPlugin {

    private static TheLabPlugin instance;

    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private PlayerManager playerManager;
    private StatsManager statsManager;
    private ScoreboardManager scoreboardManager;
    private SignManager signManager;
    private DrZuk narratorManager;
    private ArenaSetupWizard setupWizard;
    private ArenaJoinHandler arenaJoinHandler;

    @Override
    public void onEnable() {
        instance = this;

        // Config
        configManager = new ConfigManager(this);
        configManager.load();

        // Managers
        playerManager = new PlayerManager();
        arenaManager = new ArenaManager(configManager);
        arenaManager.loadArenas();
        gameManager = new GameManager(this);
        statsManager = new StatsManager(this);
        statsManager.initialize();
        scoreboardManager = new ScoreboardManager(this);
        if (configManager.isScoreboardEnabled()) {
            scoreboardManager.start();
        }
        signManager = new SignManager(this);
        signManager.load();
        narratorManager = new DrZuk(this);
        setupWizard = new ArenaSetupWizard();
        arenaJoinHandler = new ArenaJoinHandler(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);

        // Register commands
        TheLabCommand cmd = new TheLabCommand(this);
        if (getCommand("thelab") != null) {
            getCommand("thelab").setExecutor(cmd);
            getCommand("thelab").setTabCompleter(cmd);
        }

        getLogger().info("TheLab MiniGame enabled! " + arenaManager.getArenas().size() + " arenas loaded.");
    }

    @Override
    public void onDisable() {
        // Shutdown all active games and restore players
        if (gameManager != null) gameManager.shutdownAll();

        // Restore all players from arenas
        if (arenaManager != null) {
            for (com.thelab.plugin.arena.Arena arena : arenaManager.getArenas()) {
                for (org.bukkit.entity.Player p : arena.getAllParticipants()) {
                    com.thelab.plugin.player.LabPlayer lp = playerManager.getLabPlayer(p);
                    lp.restoreInventory();
                }
                arena.clearAll();
            }
        }

        if (scoreboardManager != null) scoreboardManager.stop();
        if (signManager != null) signManager.shutdown();
        if (narratorManager != null) narratorManager.shutdown();
        if (statsManager != null) statsManager.shutdown();

        getLogger().info("TheLab MiniGame disabled.");
    }

    public static TheLabPlugin getInstance() { return instance; }

    public ConfigManager getConfigManager() { return configManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return gameManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public SignManager getSignManager() { return signManager; }
    public DrZuk getNarratorManager() { return narratorManager; }
    public Optional<ArenaSetupWizard> getSetupWizard() { return Optional.ofNullable(setupWizard); }
    public ArenaJoinHandler getArenaJoinHandler() { return arenaJoinHandler; }
}

package com.thelab.plugin.arena;

import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.List;
import org.bukkit.event.block.Action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Interactive step-by-step arena setup wizard. */
public class ArenaSetupWizard {

    private final Map<UUID, WizardSession> sessions = new HashMap<>();

    /** Internal class tracking wizard progress for one admin. */
    public static class WizardSession {
        final Arena arena;
        int step;
        Location corner1;
        Location corner2;
        int experimentIndex;
        ExperimentType currentExperiment;

        WizardSession(Arena arena) {
            this.arena = arena;
            this.step = 1;
        }
    }

    /** Starts the setup wizard for an admin on a given arena. */
    public void startWizard(Player player, Arena arena) {
        WizardSession session = new WizardSession(arena);
        sessions.put(player.getUniqueId(), session);
        sendStep(player, session);
    }

    /** Cancels the wizard for a player. */
    public void cancelWizard(Player player) {
        sessions.remove(player.getUniqueId());
        MessageUtil.sendRaw(player, "&cSetup wizard cancelled.");
    }

    /** Returns true if the player is in the wizard. */
    public boolean isInWizard(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /** Handles a chat message from an admin in the wizard. */
    public boolean handleChat(Player player, String message) {
        WizardSession session = sessions.get(player.getUniqueId());
        if (session == null) return false;

        String msg = message.trim().toLowerCase();

        if (msg.equals("cancel")) {
            cancelWizard(player);
            return true;
        }

        switch (session.step) {
            case 1 -> { // Set lobby spawn
                if (msg.equals("set")) {
                    session.arena.getConfig().setLobbySpawn(player.getLocation());
                    MessageUtil.sendRaw(player, "&aLobby spawn set!");
                    session.step = 2;
                    sendStep(player, session);
                }
            }
            case 2 -> { // Set spectator spawn
                if (msg.equals("set")) {
                    session.arena.getConfig().setSpectatorSpawn(player.getLocation());
                    MessageUtil.sendRaw(player, "&aSpectator spawn set!");
                    session.step = 3;
                    sendStep(player, session);
                }
            }
            case 3 -> { // Define region
                if (msg.equals("set")) {
                    if (session.corner1 == null || session.corner2 == null) {
                        MessageUtil.sendRaw(player, "&cPlease select both corners first (left-click and right-click).");
                    } else {
                        session.arena.getConfig().setArenaMin(session.corner1);
                        session.arena.getConfig().setArenaMax(session.corner2);
                        MessageUtil.sendRaw(player, "&aArena region set!");
                        session.step = 4;
                        session.experimentIndex = 0;
                        List<ExperimentType> exps = session.arena.getConfig().getEnabledExperiments();
                        if (exps.isEmpty()) {
                            finishWizard(player, session);
                        } else {
                            session.currentExperiment = exps.get(0);
                            sendStep(player, session);
                        }
                    }
                }
            }
            default -> { // Steps 4+: experiment spawns
                if (session.step >= 4) {
                    List<ExperimentType> exps = session.arena.getConfig().getEnabledExperiments();
                    if (msg.equals("addspawn")) {
                        session.arena.getConfig().addSpawnFor(session.currentExperiment, player.getLocation());
                        MessageUtil.sendRaw(player, "&aSpawn point added for " + session.currentExperiment.getDisplayName() + "!");
                    } else if (msg.equals("done")) {
                        session.experimentIndex++;
                        if (session.experimentIndex >= exps.size()) {
                            finishWizard(player, session);
                        } else {
                            session.currentExperiment = exps.get(session.experimentIndex);
                            sendStep(player, session);
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Handles a block interaction (for corner selection in step 3). */
    public boolean handleInteract(Player player, Action action, Location clickedLocation) {
        WizardSession session = sessions.get(player.getUniqueId());
        if (session == null || session.step != 3) return false;

        if (action == Action.LEFT_CLICK_BLOCK) {
            session.corner1 = clickedLocation;
            MessageUtil.sendRaw(player, "&aCorner 1 set at " + formatLoc(clickedLocation));
            return true;
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            session.corner2 = clickedLocation;
            MessageUtil.sendRaw(player, "&aCorner 2 set at " + formatLoc(clickedLocation));
            return true;
        }
        return false;
    }

    private void sendStep(Player player, WizardSession session) {
        switch (session.step) {
            case 1 -> MessageUtil.sendRaw(player,
                    "&e[Setup Wizard] Step 1: Stand at the lobby spawn location and type &aset&e.");
            case 2 -> MessageUtil.sendRaw(player,
                    "&e[Setup Wizard] Step 2: Stand at the spectator spawn location and type &aset&e.");
            case 3 -> MessageUtil.sendRaw(player,
                    "&e[Setup Wizard] Step 3: &aLeft-click&e a block for corner 1, &aright-click&e for corner 2, then type &aset&e.");
            default -> {
                if (session.currentExperiment != null) {
                    MessageUtil.sendRaw(player,
                            "&e[Setup Wizard] Step " + session.step + ": Set spawn points for &a"
                                    + session.currentExperiment.getDisplayName()
                                    + "&e. Type &aaddspawn&e at each spawn, then &adone&e when finished.");
                }
            }
        }
        MessageUtil.sendRaw(player, "&7Type &ccancel&7 at any time to exit the wizard.");
    }

    private void finishWizard(Player player, WizardSession session) {
        sessions.remove(player.getUniqueId());
        MessageUtil.sendRaw(player, "&aSetup complete! Use &e/thelab enable " + session.arena.getId() + "&a to enable the arena.");
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}

package com.thelab.plugin.commands;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.experiment.ExperimentType;
import com.thelab.plugin.stats.PlayerStats;
import com.thelab.plugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/** Main /thelab command executor with all subcommands. */
public class TheLabCommand implements CommandExecutor, TabCompleter {

    private final TheLabPlugin plugin;

    public TheLabCommand(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join" -> handleJoin(sender, args);
            case "quickjoin" -> handleQuickJoin(sender);
            case "leave" -> handleLeave(sender);
            case "spectate" -> handleSpectate(sender, args);
            case "stats" -> handleStats(sender, args);
            case "leaderboard", "lb" -> handleLeaderboard(sender, args);
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setup" -> handleSetup(sender, args);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "setlobby" -> handleSetLobby(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setmainlobby" -> handleSetMainLobby(sender);
            case "forcestart" -> handleForceStart(sender, args);
            case "enable" -> handleEnable(sender, args);
            case "disable" -> handleDisable(sender, args);
            case "reload" -> handleReload(sender);
            case "npc" -> handleNpc(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) return;
        Player player = (Player) sender;
        if (!player.hasPermission("thelab.join")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(player, "&cUsage: /thelab join <arena>"); return; }
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(
                arena -> plugin.getArenaJoinHandler().joinArena(player, arena),
                () -> MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleQuickJoin(CommandSender sender) {
        if (!checkPlayer(sender)) return;
        Player player = (Player) sender;
        if (!player.hasPermission("thelab.join")) { noPerms(sender); return; }
        plugin.getArenaManager().getJoinableArena().ifPresentOrElse(
                arena -> plugin.getArenaJoinHandler().joinArena(player, arena),
                () -> MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.no-arenas")));
    }

    private void handleLeave(CommandSender sender) {
        if (!checkPlayer(sender)) return;
        plugin.getArenaJoinHandler().leaveArena((Player) sender);
    }

    private void handleSpectate(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) return;
        Player player = (Player) sender;
        if (!player.hasPermission("thelab.spectate")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(player, "&cUsage: /thelab spectate <arena>"); return; }
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(arena -> {
            arena.addSpectator(player);
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            if (arena.getSpectatorSpawn() != null) player.teleport(arena.getSpectatorSpawn());
            MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("spectator.joined"));
        }, () -> MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.stats")) { noPerms(sender); return; }
        UUID target;
        String targetName;
        if (args.length >= 2) {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[1]);
            if (op == null) { MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.player-not-found", "player", args[1])); return; }
            target = op.getUniqueId();
            targetName = op.getName() != null ? op.getName() : args[1];
        } else if (sender instanceof Player player) {
            target = player.getUniqueId();
            targetName = player.getName();
        } else {
            MessageUtil.sendRaw(sender, "&cSpecify a player name."); return;
        }
        PlayerStats stats = plugin.getStatsManager().getStats(target);
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.header", "player", targetName)));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.games-played", "value", String.valueOf(stats.getGamesPlayed()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.wins", "value", String.valueOf(stats.getWins()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.losses", "value", String.valueOf(stats.getLosses()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.kills", "value", String.valueOf(stats.getKills()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.deaths", "value", String.valueOf(stats.getDeaths()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.points", "value", String.valueOf(stats.getPointsEarned()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.kdr", "value", String.valueOf(stats.getKdr()))));
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("stats.winrate", "value", String.valueOf(stats.getWinRate()))));
    }

    private void handleLeaderboard(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.leaderboard")) { noPerms(sender); return; }
        String cat = args.length >= 2 ? args[1] : "wins";
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("leaderboard.header", "category", cat)));
        List<PlayerStats> top = plugin.getStatsManager().getLeaderboard(cat, 10);
        for (int i = 0; i < top.size(); i++) {
            PlayerStats s = top.get(i);
            String value = switch (cat.toLowerCase()) {
                case "kills" -> String.valueOf(s.getKills());
                case "points" -> String.valueOf(s.getPointsEarned());
                case "games" -> String.valueOf(s.getGamesPlayed());
                default -> String.valueOf(s.getWins());
            };
            sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("leaderboard.entry",
                    "rank", String.valueOf(i + 1), "player", s.getPlayerName(), "value", value)));
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("arena.list-header")));
        for (Arena arena : plugin.getArenaManager().getArenas()) {
            sender.sendMessage(MessageUtil.toComponent(plugin.getConfigManager().getMessage("arena.list-entry",
                    "id", arena.getId(),
                    "state", arena.getState().getDisplayName(),
                    "current", String.valueOf(arena.getPlayerCount()),
                    "max", String.valueOf(arena.getMaxPlayers()))));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 4) { MessageUtil.sendRaw(sender, "&cUsage: /thelab create <id> <min> <max>"); return; }
        String id = args[1];
        if (plugin.getArenaManager().arenaExists(id)) {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-already-exists")); return;
        }
        try {
            int min = Integer.parseInt(args[2]);
            int max = Integer.parseInt(args[3]);
            plugin.getArenaManager().createArena(id, min, max);
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.created", "arena", id));
        } catch (NumberFormatException e) {
            MessageUtil.sendRaw(sender, "&cInvalid number.");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(sender, "&cUsage: /thelab delete <id>"); return; }
        if (plugin.getArenaManager().deleteArena(args[1])) {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.deleted", "arena", args[1]));
        } else {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1]));
        }
    }

    private void handleSetup(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) return;
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(sender, "&cUsage: /thelab setup <id>"); return; }
        Player player = (Player) sender;
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(
                arena -> plugin.getSetupWizard().ifPresent(wizard -> wizard.startWizard(player, arena)),
                () -> MessageUtil.sendRaw(player, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 3) { MessageUtil.sendRaw(sender, "&cUsage: /thelab add <id> <experiment>"); return; }
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(arena -> {
            ExperimentType type = ExperimentType.fromConfigKey(args[2]);
            if (type == null) { MessageUtil.sendRaw(sender, "&cUnknown experiment type: " + args[2]); return; }
            arena.getConfig().addExperiment(type);
            plugin.getArenaManager().saveArenas();
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.experiment-added", "experiment", type.getDisplayName(), "arena", args[1]));
        }, () -> MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 3) { MessageUtil.sendRaw(sender, "&cUsage: /thelab remove <id> <experiment>"); return; }
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(arena -> {
            ExperimentType type = ExperimentType.fromConfigKey(args[2]);
            if (type == null) { MessageUtil.sendRaw(sender, "&cUnknown experiment type: " + args[2]); return; }
            arena.getConfig().removeExperiment(type);
            plugin.getArenaManager().saveArenas();
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.experiment-removed", "experiment", type.getDisplayName(), "arena", args[1]));
        }, () -> MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleSetLobby(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) return;
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(sender, "&cUsage: /thelab setlobby <id>"); return; }
        Player player = (Player) sender;
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(arena -> {
            arena.getConfig().setLobbySpawn(player.getLocation());
            plugin.getArenaManager().saveArenas();
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.lobby-set", "arena", args[1]));
        }, () -> MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) return;
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 3) { MessageUtil.sendRaw(sender, "&cUsage: /thelab setspawn <id> <experiment>"); return; }
        Player player = (Player) sender;
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(arena -> {
            ExperimentType type = ExperimentType.fromConfigKey(args[2]);
            if (type == null) { MessageUtil.sendRaw(sender, "&cUnknown experiment type: " + args[2]); return; }
            arena.getConfig().addSpawnFor(type, player.getLocation());
            plugin.getArenaManager().saveArenas();
            MessageUtil.sendRaw(sender, "&aSpawn point added for " + type.getDisplayName() + " in arena " + args[1] + ".");
        }, () -> MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleSetMainLobby(CommandSender sender) {
        if (!checkPlayer(sender)) return;
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        Player player = (Player) sender;
        org.bukkit.Location loc = player.getLocation();
        plugin.getConfigManager().getConfig().set("main-lobby.world", loc.getWorld().getName());
        plugin.getConfigManager().getConfig().set("main-lobby.x", loc.getX());
        plugin.getConfigManager().getConfig().set("main-lobby.y", loc.getY());
        plugin.getConfigManager().getConfig().set("main-lobby.z", loc.getZ());
        plugin.getConfigManager().getConfig().set("main-lobby.yaw", (double) loc.getYaw());
        plugin.getConfigManager().getConfig().set("main-lobby.pitch", (double) loc.getPitch());
        plugin.saveConfig();
        MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.main-lobby-set"));
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(sender, "&cUsage: /thelab forcestart <id>"); return; }
        plugin.getArenaManager().getArena(args[1]).ifPresentOrElse(arena -> {
            plugin.getGameManager().forceStart(arena);
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.force-started", "arena", args[1]));
        }, () -> MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1])));
    }

    private void handleEnable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(sender, "&cUsage: /thelab enable <id>"); return; }
        if (plugin.getArenaManager().enableArena(args[1])) {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.enabled", "arena", args[1]));
        } else {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1]));
        }
    }

    private void handleDisable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        if (args.length < 2) { MessageUtil.sendRaw(sender, "&cUsage: /thelab disable <id>"); return; }
        if (plugin.getArenaManager().disableArena(args[1])) {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.disabled", "arena", args[1]));
        } else {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.arena-not-found", "arena", args[1]));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("thelab.admin")) { noPerms(sender); return; }
        plugin.getConfigManager().load();
        MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("arena.reloaded"));
    }

    private void handleNpc(CommandSender sender, String[] args) {
        // Basic NPC command stub
        MessageUtil.sendRaw(sender, "&eNPC management requires ProtocolLib. Use /thelab npc add/remove <arenaId>.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.toComponent("&e&l=== TheLab MiniGame ==="));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab join <arena> &7- Join an arena"));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab quickjoin &7- Quick join best arena"));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab leave &7- Leave current arena"));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab spectate <arena> &7- Spectate an arena"));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab stats [player] &7- View stats"));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab leaderboard [category] &7- View leaderboard"));
        sender.sendMessage(MessageUtil.toComponent("&e/thelab list &7- List all arenas"));
        if (sender.hasPermission("thelab.admin")) {
            sender.sendMessage(MessageUtil.toComponent("&6/thelab create <id> <min> <max> &7- Create arena"));
            sender.sendMessage(MessageUtil.toComponent("&6/thelab delete <id> &7- Delete arena"));
            sender.sendMessage(MessageUtil.toComponent("&6/thelab setup <id> &7- Setup wizard"));
            sender.sendMessage(MessageUtil.toComponent("&6/thelab enable/disable <id> &7- Enable/disable arena"));
            sender.sendMessage(MessageUtil.toComponent("&6/thelab forcestart <id> &7- Force start game"));
            sender.sendMessage(MessageUtil.toComponent("&6/thelab reload &7- Reload configs"));
        }
    }

    private boolean checkPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.player-only"));
            return false;
        }
        return true;
    }

    private void noPerms(CommandSender sender) {
        MessageUtil.sendRaw(sender, plugin.getConfigManager().getMessage("errors.no-permission"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("join", "quickjoin", "leave", "spectate", "stats", "leaderboard", "list"));
            if (sender.hasPermission("thelab.admin")) {
                subs.addAll(List.of("create", "delete", "setup", "add", "remove", "setlobby", "setspawn", "setmainlobby", "forcestart", "enable", "disable", "reload", "npc"));
            }
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            List<String> arenas = plugin.getArenaManager().getArenas().stream()
                    .map(Arena::getId).collect(Collectors.toList());
            switch (args[0].toLowerCase()) {
                case "join", "delete", "setup", "setlobby", "forcestart", "enable", "disable",
                        "spectate", "add", "remove" -> { return filter(arenas, args[1]); }
                case "stats", "leaderboard" -> { return filter(List.of("wins", "kills", "points", "games"), args[1]); }
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")
                    || args[0].equalsIgnoreCase("setspawn")) {
                return filter(Arrays.stream(ExperimentType.values()).map(ExperimentType::getConfigKey).collect(Collectors.toList()), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}

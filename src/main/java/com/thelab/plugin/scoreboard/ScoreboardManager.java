package com.thelab.plugin.scoreboard;

import com.thelab.plugin.TheLabPlugin;
import com.thelab.plugin.arena.Arena;
import com.thelab.plugin.arena.ArenaState;
import com.thelab.plugin.game.ScoreManager;
import com.thelab.plugin.utils.MessageUtil;
import com.thelab.plugin.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.text.SimpleDateFormat;
import java.util.*;

/** Non-flickering scoreboard manager using team-based approach. */
public class ScoreboardManager {

    private final TheLabPlugin plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, Map<Integer, Team>> playerTeams = new HashMap<>();
    private BukkitTask updateTask;

    public ScoreboardManager(TheLabPlugin plugin) {
        this.plugin = plugin;
    }

    /** Starts the periodic scoreboard update task. */
    public void start() {
        int interval = plugin.getConfigManager().getScoreboardUpdateInterval();
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Arena arena : plugin.getArenaManager().getArenas()) {
                    for (Player p : arena.getAllParticipants()) {
                        updateScoreboard(p, arena);
                    }
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    /** Stops the update task. */
    public void stop() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
        // Remove all scoreboards
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        playerScoreboards.clear();
        playerTeams.clear();
    }

    /** Creates or updates a player's scoreboard. */
    public void updateScoreboard(Player player, Arena arena) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) return;

        List<String> lines = buildLines(player, arena);
        String title = getTitle(arena);

        Scoreboard sb = playerScoreboards.computeIfAbsent(player.getUniqueId(),
                uuid -> createScoreboard(player, title));

        Objective obj = sb.getObjective("thelab");
        if (obj == null) {
            obj = sb.registerNewObjective("thelab", Criteria.DUMMY,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        // Update title
        obj.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(title));

        // Update lines using teams (no flickering)
        Map<Integer, Team> teams = playerTeams.computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>());

        // First, ensure enough teams exist
        for (int i = 0; i < lines.size(); i++) {
            int score = lines.size() - i;
            Team team = teams.computeIfAbsent(score, s -> {
                String teamName = "tl_" + s;
                Team t = sb.getTeam(teamName);
                if (t == null) t = sb.registerNewTeam(teamName);
                // Add entry
                String entry = getEntry(s);
                if (!t.hasEntry(entry)) t.addEntry(entry);
                obj.getScore(entry).setScore(s);
                return t;
            });
            team.prefix(LegacyComponentSerializer.legacyAmpersand().deserialize(lines.get(i)));
        }

        player.setScoreboard(sb);
    }

    private Scoreboard createScoreboard(Player player, String title) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        return sb;
    }

    /** Removes a player's scoreboard. */
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        playerTeams.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private String getTitle(Arena arena) {
        ArenaState state = arena.getState();
        return switch (state) {
            case GAME_END -> plugin.getConfigManager().getScoreboard().getString("end.title", "&e&lTHE LAB");
            case EXPERIMENT_PLAY, EXPERIMENT_INTRO, EXPERIMENT_RESULTS -> 
                plugin.getConfigManager().getScoreboard().getString("game.title", "&e&lTHE LAB");
            default -> plugin.getConfigManager().getScoreboard().getString("lobby.title", "&e&lTHE LAB");
        };
    }

    private List<String> buildLines(Player player, Arena arena) {
        ArenaState state = arena.getState();
        String date = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

        return switch (state) {
            case EXPERIMENT_PLAY, EXPERIMENT_INTRO, EXPERIMENT_RESULTS -> buildGameLines(player, arena, date);
            case GAME_END -> buildEndLines(player, arena, date);
            default -> buildLobbyLines(player, arena, date);
        };
    }

    private List<String> buildLobbyLines(Player player, Arena arena, String date) {
        List<String> template = plugin.getConfigManager().getScoreboard().getStringList("lobby.lines");
        if (template.isEmpty()) {
            template = List.of("&7" + date, "", "&fMap: &a" + arena.getDisplayName(),
                    "&fPlayers: &a" + arena.getPlayerCount() + "/" + arena.getMaxPlayers(),
                    "&fStatus: " + arena.getState().getColoredName(), "", "&ewww.yourserver.com");
        }
        List<String> result = new ArrayList<>();
        for (String line : template) {
            result.add(MessageUtil.colorize(line
                    .replace("{date}", date)
                    .replace("{arena_name}", arena.getDisplayName())
                    .replace("{current}", String.valueOf(arena.getPlayerCount()))
                    .replace("{max}", String.valueOf(arena.getMaxPlayers()))
                    .replace("{state}", arena.getState().getDisplayName())));
        }
        return result;
    }

    private List<String> buildGameLines(Player player, Arena arena, String date) {
        Optional<ScoreManager> scoresOpt = plugin.getGameManager().getScoreManager(arena);
        Optional<com.thelab.plugin.game.ExperimentRotation> rotOpt = plugin.getGameManager().getRotation(arena);

        String expName = rotOpt.map(r -> {
            var cur = r.getCurrentExperiment();
            return cur != null ? cur.getDisplayName() : "Unknown";
        }).orElse("Unknown");
        String round = rotOpt.map(r -> r.getRound() + "/" + r.getTotalRounds()).orElse("1/3");
        String score = scoresOpt.map(s -> String.valueOf(s.getScore(player.getUniqueId()))).orElse("0");
        String rank = scoresOpt.map(s -> "#" + s.getExperimentRank(player.getUniqueId())).orElse("#?");

        // Top 3 players
        String p1 = "-", p2 = "-", p3 = "-";
        String s1 = "0", s2 = "0", s3 = "0";
        if (scoresOpt.isPresent()) {
            List<UUID> ranking = scoresOpt.get().getTotalRanking();
            if (ranking.size() > 0) { Player bp = Bukkit.getPlayer(ranking.get(0)); p1 = bp != null ? bp.getName() : "?"; s1 = String.valueOf(scoresOpt.get().getTotalScore(ranking.get(0))); }
            if (ranking.size() > 1) { Player bp = Bukkit.getPlayer(ranking.get(1)); p2 = bp != null ? bp.getName() : "?"; s2 = String.valueOf(scoresOpt.get().getTotalScore(ranking.get(1))); }
            if (ranking.size() > 2) { Player bp = Bukkit.getPlayer(ranking.get(2)); p3 = bp != null ? bp.getName() : "?"; s3 = String.valueOf(scoresOpt.get().getTotalScore(ranking.get(2))); }
        }

        List<String> template = plugin.getConfigManager().getScoreboard().getStringList("game.lines");
        if (template.isEmpty()) {
            return List.of("&7Round " + round, "", "&fExperiment: &a" + expName,
                    "&fYour Score: &a" + score, "&fYour Rank: &e" + rank, "", "&ewww.yourserver.com");
        }
        List<String> result = new ArrayList<>();
        final String fp1 = p1, fp2 = p2, fp3 = p3, fs1 = s1, fs2 = s2, fs3 = s3;
        for (String line : template) {
            result.add(MessageUtil.colorize(line
                    .replace("{round}", round)
                    .replace("{experiment_name}", expName)
                    .replace("{score}", score)
                    .replace("{rank}", rank)
                    .replace("{player1}", fp1).replace("{score1}", fs1)
                    .replace("{player2}", fp2).replace("{score2}", fs2)
                    .replace("{player3}", fp3).replace("{score3}", fs3)));
        }
        return result;
    }

    private List<String> buildEndLines(Player player, Arena arena, String date) {
        Optional<ScoreManager> scoresOpt = plugin.getGameManager().getScoreManager(arena);
        String winner = "-";
        String p1 = "-", p2 = "-", p3 = "-";
        String s1 = "0", s2 = "0", s3 = "0";
        String myRank = "?", myScore = "0";
        if (scoresOpt.isPresent()) {
            List<UUID> ranking = scoresOpt.get().getTotalRanking();
            if (!ranking.isEmpty()) {
                Player wp = Bukkit.getPlayer(ranking.get(0));
                winner = wp != null ? wp.getName() : "?";
            }
            if (ranking.size() > 0) { Player bp = Bukkit.getPlayer(ranking.get(0)); p1 = bp != null ? bp.getName() : "?"; s1 = String.valueOf(scoresOpt.get().getTotalScore(ranking.get(0))); }
            if (ranking.size() > 1) { Player bp = Bukkit.getPlayer(ranking.get(1)); p2 = bp != null ? bp.getName() : "?"; s2 = String.valueOf(scoresOpt.get().getTotalScore(ranking.get(1))); }
            if (ranking.size() > 2) { Player bp = Bukkit.getPlayer(ranking.get(2)); p3 = bp != null ? bp.getName() : "?"; s3 = String.valueOf(scoresOpt.get().getTotalScore(ranking.get(2))); }
            myRank = String.valueOf(scoresOpt.get().getTotalRank(player.getUniqueId()));
            myScore = String.valueOf(scoresOpt.get().getTotalScore(player.getUniqueId()));
        }
        List<String> template = plugin.getConfigManager().getScoreboard().getStringList("end.lines");
        if (template.isEmpty()) {
            return List.of("&7Game Over!", "", "&6Winner: " + winner, "&fYour Rank: &e#" + myRank, "&fYour Score: &a" + myScore, "", "&ewww.yourserver.com");
        }
        final String fw = winner, fp1 = p1, fp2 = p2, fp3 = p3, fs1 = s1, fs2 = s2, fs3 = s3, fr = myRank, fms = myScore;
        List<String> result = new ArrayList<>();
        for (String line : template) {
            result.add(MessageUtil.colorize(line
                    .replace("{winner}", fw)
                    .replace("{player1}", fp1).replace("{total_score1}", fs1)
                    .replace("{player2}", fp2).replace("{total_score2}", fs2)
                    .replace("{player3}", fp3).replace("{total_score3}", fs3)
                    .replace("{rank}", fr).replace("{score}", fms)));
        }
        return result;
    }

    /** Returns a unique color-code string to use as scoreboard entry for a given score. */
    private String getEntry(int score) {
        String[] codes = {"\u00a70", "\u00a71", "\u00a72", "\u00a73", "\u00a74", "\u00a75",
                "\u00a76", "\u00a77", "\u00a78", "\u00a79", "\u00a7a", "\u00a7b",
                "\u00a7c", "\u00a7d", "\u00a7e", "\u00a7f"};
        return codes[score % codes.length] + "\u00a7r";
    }
}

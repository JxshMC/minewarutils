package com.jxsh.misc;

import me.clip.placeholderapi.PlaceholderAPI;
import dev.dejvokep.boostedyaml.route.Route;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardManager implements Listener {

    private final JxshMisc plugin;

    // Config values
    private boolean enabled;
    private String title;
    private List<String> lines;
    private int updateInterval;
    private Map<String, String> customPlaceholders;
    // Dynamic Placeholders configuration
    private Map<String, String> dynamicTempOp; // Stores config for temp-op

    private BukkitTask updateTask;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    public ScoreboardManager(JxshMisc plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        try {
            dev.dejvokep.boostedyaml.YamlDocument config = plugin.getConfigManager().getScoreboard();
            if (config == null) {
                plugin.getLogger().warning("Scoreboard: scoreboard.yml failed to load!");
                return;
            }

            enabled = config.getBoolean("enabled", true);
            title = config.getString("title", "<gradient:#55ffff:#00aa00><b>My Server</b></gradient>");
            lines = config.getStringList("Scoreboard");
            updateInterval = config.getInt("update-interval", 20);

            customPlaceholders = new HashMap<>();
            if (config.isSection("placeholders")) {
                dev.dejvokep.boostedyaml.block.implementation.Section section = config.getSection("placeholders");
                for (Object key : section.getKeys()) {
                    String sKey = key.toString();
                    String sVal = section.getString(Route.from(key), "");
                    customPlaceholders.put(sKey, sVal);
                }
            }

            // Load dynamic-placeholders section
            dynamicTempOp = new HashMap<>();
            if (config.isSection("dynamic-placeholders.temp-op")) {
                dev.dejvokep.boostedyaml.block.implementation.Section section = config
                        .getSection("dynamic-placeholders.temp-op");
                for (Object key : section.getKeys()) {
                    dynamicTempOp.put(key.toString(), section.getString(Route.from(key)));
                }
            }

            if (enabled) {
                startTask();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    setupScoreboard(player);
                }
            } else {
                stopTask();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    playerScoreboards.remove(player.getUniqueId());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load scoreboard config!");
            e.printStackTrace();
        }
    }

    private void startTask() {
        stopTask();
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }

    private void stopTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (enabled) {
            setupScoreboard(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerScoreboards.remove(event.getPlayer().getUniqueId());
    }

    private void setupScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY,
                MiniMessage.miniMessage().deserialize(title)); // Initial title, updated dynamically later

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        updateScoreboard(player);
    }

    private void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null)
            return;

        Objective objective = scoreboard.getObjective("sidebar");
        if (objective == null)
            return;

        // Update Title
        String parsedTitle = applyPlaceholders(title, player);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            parsedTitle = PlaceholderAPI.setPlaceholders(player, parsedTitle);
        }
        objective.displayName(MiniMessage.miniMessage().deserialize(parsedTitle));

        // Process Lines (Dynamic Logic)
        List<String> processedLines = new ArrayList<>();

        for (String line : lines) {
            // 1. {temp-op} Handler
            if (line.contains("{temp-op}")) {
                if (!processTempOpLine(line, player, processedLines)) {
                    continue; // Line removed
                }
                // processTempOpLine handles adding to processedLines if valid
            } else {
                // Normal line processing
                String processed = applyPlaceholders(line, player);
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    processed = PlaceholderAPI.setPlaceholders(player, processed);
                }
                processNewline(processed, processedLines);
            }
        }

        // Assign Scores (Reverse Loop)
        // Score 15 (Top) -> Score 1 (Bottom)
        // Adjust scores based on list size
        int score = processedLines.size();

        // Clean up old teams/entries to prevent ghosts?
        // Ideally we assume max lines 15. We can clear scores < current size?
        // Or cleaner: Reset all scores/teams.
        // For performance, we update existing teams.
        // We need to clear scores that are no longer used (e.g. if list shrank).

        for (String entry : scoreboard.getEntries()) {
            if (objective.getScore(entry).getScore() > processedLines.size()) {
                scoreboard.resetScores(entry);
            }
        }

        // Since we map lines to scores 1..N, we can just overwrite.
        // But if we had 10 lines and now 9, score 10 needs to be removed.
        // Better: Clear all scores for this objective? No, that flickers.

        // We iterate 15 down to 1.
        for (int i = 0; i < 15; i++) {
            // If we have a line for this slot (index = i from top?)
            // Standard: index 0 is top line -> Highest Score.
            int lineIndex = i;
            int currentScore = processedLines.size() - i;

            if (lineIndex < processedLines.size()) {
                updateLine(scoreboard, objective, currentScore, processedLines.get(lineIndex));
            } else {
                // Remove this score/line if it exists
                removeLine(scoreboard, currentScore);
            }
        }
    }

    // Returns false if line should be removed
    private boolean processTempOpLine(String line, Player player, List<String> output) {
        String timeLeft = "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            timeLeft = PlaceholderAPI.setPlaceholders(player, "%tempop_time_left%");
        }

        // Check dynamic placeholders config
        String replacement = null;

        // Logic:
        // 1. Exact Match (e.g. "Expire-Relog")
        if (dynamicTempOp != null && dynamicTempOp.containsKey(timeLeft)) {
            replacement = dynamicTempOp.get(timeLeft);
        } else {
            // 2. Regex Match for Time (d, h, m, s)
            // "If output contains a time pattern... replace with 'Expires in...'"
            // We check checks if it contains digits followed by d/h/m/s
            if (timeLeft.matches(".*\\d+[dhms].*")) {
                // It's a time. Use "time" key from config if exists, or default?
                // User said: "replace {temp-op} with the 'Expires in...' string"
                // I assume there's a key in config for this case?
                // Let's assume key is "time-format" or similar?
                // Or maybe the user meant "If output contains time... replace with the
                // formatted string ITSELF"?
                // "replace {temp-op} with the 'Expires in...' string" implies a specific string
                // from config.
                // Let's look for a key called "active" or "time" in
                // dynamic-placeholders.temp-op
                if (dynamicTempOp != null && dynamicTempOp.containsKey("active")) {
                    replacement = dynamicTempOp.get("active").replace("%time%", timeLeft);
                } else {
                    // Fallback: Just display the time?
                    replacement = timeLeft;
                }
            }
        }

        if (replacement == null) {
            // "Line Removal: If ... does not match ... remove"
            return false;
        }

        String processed = line.replace("{temp-op}", replacement);
        processed = applyPlaceholders(processed, player);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }

        processNewline(processed, output);
        return true;
    }

    private void processNewline(String line, List<String> output) {
        if (line.contains("{newline}")) {
            String[] parts = line.split("\\{newline\\}");
            for (String part : parts) {
                // "If text after {newline}, display it... otherwise blank spacer"
                // Split handles this. "Text{newline}" -> ["Text", ""] (if limit is negative)
                // String.split(regex) discards trailing empty strings by default.
                // We want to keep them.
                output.add(part);
            }
        } else if (line.contains("\\n")) { // support \n just in case
            String[] parts = line.split("\\\\n");
            for (String part : parts)
                output.add(part);
        } else {
            output.add(line);
        }
    }

    private void updateLine(Scoreboard sb, Objective obj, int score, String text) {
        String teamName = "line_" + score;
        Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
            String entry = "§" + Integer.toHexString(score); // Unique entry
            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
        }
        // Update prefix
        // Handle Legacy + MiniMessage mixed? Use serializer?
        // text likely contains MiniMessage tags.
        team.prefix(MiniMessage.miniMessage().deserialize(text));
    }

    private void removeLine(Scoreboard sb, int score) {
        String teamName = "line_" + score;
        Team team = sb.getTeam(teamName);
        if (team != null) {
            for (String entry : team.getEntries()) {
                sb.resetScores(entry);
            }
            team.unregister();
        }
    }

    private String applyPlaceholders(String text, Player player) {
        String result = text;
        if (customPlaceholders != null) {
            for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}

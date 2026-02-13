package com.jxsh.misc;

import com.jxsh.misc.utils.CenteringManager;
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

    // Stage 1 Config
    private Map<String, String> staticPlaceholders; // "placeholders" section
    private Map<String, Map<String, String>> replacements; // "Replacements" section

    // Stage 2 Config
    private Map<String, String> dynamicTempOp; // "dynamic-placeholders.temp-op"

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

            // Load Stage 1: Static Placeholders
            staticPlaceholders = new HashMap<>();
            if (config.isSection("placeholders")) {
                dev.dejvokep.boostedyaml.block.implementation.Section section = config.getSection("placeholders");
                for (Object key : section.getKeys()) {
                    staticPlaceholders.put(key.toString(), section.getString(Route.from(key), ""));
                }
            }

            // Load Stage 1: Replacements
            replacements = new HashMap<>();
            if (config.isSection("Replacements")) {
                dev.dejvokep.boostedyaml.block.implementation.Section replSection = config.getSection("Replacements");
                for (Object key : replSection.getKeys()) {
                    // Key is the PAPI string, e.g. "%luckperms_prefix%"
                    String baseKey = key.toString();
                    if (replSection.isSection(Route.from(key))) {
                        dev.dejvokep.boostedyaml.block.implementation.Section inner = replSection
                                .getSection(Route.from(key));
                        Map<String, String> innerMap = new HashMap<>();
                        for (Object innerKey : inner.getKeys()) {
                            // Inner key is the target value to match (e.g. "")
                            innerMap.put(innerKey.toString(), inner.getString(Route.from(innerKey), ""));
                        }
                        replacements.put(baseKey, innerMap);
                    }
                }
            }

            // Load Stage 2: Dynamic Placeholders
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
                MiniMessage.miniMessage().deserialize(processTitle(title, player)));

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        updateScoreboard(player);
    }

    private String processTitle(String text, Player player) {
        // Simple processing for title: Static -> PAPI. No complex dynamic logic usually
        // needed for title base.
        String processed = applyStaticPlaceholders(text);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }
        return processed;
    }

    private void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null)
            return;

        Objective objective = scoreboard.getObjective("sidebar");
        if (objective == null)
            return;

        // Update Title
        String parsedTitle = processTitle(title, player);
        objective.displayName(MiniMessage.miniMessage().deserialize(parsedTitle));

        // --- Processing Pipeline ---
        List<String> finalLines = new ArrayList<>();

        for (String rawLine : lines) {
            // Stage 1: Static Logic (Placeholders + Replacements)
            String stage1 = applyStage1(rawLine, player);

            // Stage 2: Dynamic Logic ({temp-op} triggers)
            // If returns null, line is killed.
            String stage2 = applyStage2(stage1, player);
            if (stage2 == null)
                continue; // Kill Switch Pipeline

            // Visual Engine: {newline} splitter
            // If line contains {newline}, split it.
            // Note: split() can return array. We need to handle list insertion.
            List<String> splitLines = applyNewlineSplit(stage2);

            // Add to final list
            for (String split : splitLines) {
                // Final PAPI pass for any remaining placeholders (standard ones)
                String finalPass = split;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    finalPass = PlaceholderAPI.setPlaceholders(player, finalPass);
                }

                // Visual Engine: {centre}
                finalPass = applyCentering(finalPass);

                finalLines.add(finalPass);
            }
        }

        // --- Update Scoreboard Teams ---
        // Iterate 15 down to 1 (or size)

        // Cleanup: Reset scores > size
        for (String entry : scoreboard.getEntries()) {
            org.bukkit.scoreboard.Score sc = objective.getScore(entry);
            if (sc.isScoreSet() && sc.getScore() > finalLines.size()) {
                scoreboard.resetScores(entry);
            }
        }

        // Assign lines
        for (int i = 0; i < 15; i++) {
            int lineIndex = i;
            int score = finalLines.size() - i;

            if (score < 1)
                break; // Should not happen if loop based on 15

            if (lineIndex < finalLines.size()) {
                updateLine(scoreboard, objective, score, finalLines.get(lineIndex));
            } else {
                removeLine(scoreboard, score);
            }
        }
    }

    // --- Stage 1 ---
    private String applyStage1(String text, Player player) {
        // 1. Static Placeholders
        String result = applyStaticPlaceholders(text);

        // 2. Replacements Engine
        if (replacements != null) {
            for (Map.Entry<String, Map<String, String>> entry : replacements.entrySet()) {
                String papiKey = entry.getKey(); // e.g. %luckperms_prefix%
                Map<String, String> rules = entry.getValue();

                if (result.contains(papiKey)) {
                    String resolved = "";
                    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                        resolved = PlaceholderAPI.setPlaceholders(player, papiKey);
                    }
                    if (resolved == null)
                        resolved = "";

                    // Logic: If resolved is empty "", check for rule ""
                    if (resolved.isEmpty() && rules.containsKey("")) {
                        result = result.replace(papiKey, rules.get(""));
                    } else if (rules.containsKey(resolved)) {
                        result = result.replace(papiKey, rules.get(resolved));
                    } else {
                        // Default logic: just replace with resolved value
                        result = result.replace(papiKey, resolved);
                    }
                }
            }
        }
        return result;
    }

    // --- Stage 2 ---
    // Returns null if line should be removed
    private String applyStage2(String text, Player player) {
        if (!text.contains("{temp-op}")) {
            return text;
        }

        String timeLeft = "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            timeLeft = PlaceholderAPI.setPlaceholders(player, "%tempop_time_left%");
        }

        // Dynamic Logic
        // Check 1: "Expire-Relog"
        if (timeLeft.equalsIgnoreCase("Expire-Relog") && dynamicTempOp.containsKey("Expire-Relog")) {
            return text.replace("{temp-op}", dynamicTempOp.get("Expire-Relog"));
        }

        // Check 2: Time Pattern (digits + d/h/m/s)
        if (timeLeft.matches(".*\\d+[dhms].*")) {
            if (dynamicTempOp.containsKey("active")) {
                return text.replace("{temp-op}", dynamicTempOp.get("active").replace("%time%", timeLeft));
            }
            return text.replace("{temp-op}", timeLeft);
        }

        // Check 3: Kill Switch
        // If we reached here, it's neither Relog nor Active Time.
        // Likely empty or "not active".
        // The rule says: "If the OP status is inactive/empty, remove the line entirely"
        return null;
    }

    // --- Visual Engine ---
    private List<String> applyNewlineSplit(String text) {
        List<String> list = new ArrayList<>();
        if (text.contains("{newline}")) {
            String[] parts = text.split("\\{newline\\}");
            for (String p : parts)
                list.add(p);
        } else if (text.contains("\n")) {
            String[] parts = text.split("\n");
            for (String p : parts)
                list.add(p);
        } else {
            list.add(text);
        }
        return list;
    }

    private String applyCentering(String text) {
        if (text.contains("{centre}")) {
            String clean = text.replace("{centre}", "");
            // Use CenteringManager
            return CenteringManager.getCenteredMessage(clean);
        }
        return text;
    }

    // --- Helpers ---

    private void updateLine(Scoreboard sb, Objective obj, int score, String text) {
        String teamName = "line_" + score;
        Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
            String entry = "§" + Integer.toHexString(score); // Unique entry 0-F
            if (score > 15)
                entry = "§" + score; // Fallback for >15, though unlikely
            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
        }
        try {
            team.prefix(MiniMessage.miniMessage().deserialize(text));
        } catch (Exception e) {
            // Fallback for non-minimessage or errors
            team.prefix(net.kyori.adventure.text.Component.text(text));
        }
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

    private String applyStaticPlaceholders(String text) {
        if (staticPlaceholders == null || staticPlaceholders.isEmpty())
            return text;
        String result = text;
        for (Map.Entry<String, String> entry : staticPlaceholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

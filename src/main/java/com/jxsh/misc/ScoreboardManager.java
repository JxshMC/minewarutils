package com.jxsh.misc;

import me.clip.placeholderapi.PlaceholderAPI;
import dev.dejvokep.boostedyaml.route.Route;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        // V10: Use Adventure component for title
        Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY,
                MiniMessage.miniMessage().deserialize(processTitle(title, player)));

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Hide Numbers via Paper API if available
        try {
            // Use reflection or direct check if compiling against paper API
            // objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        } catch (Throwable t) {
            // Fallback: older version or not Paper. Numbers will default to 0.
        }

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
        try {
            objective.displayName(MiniMessage.miniMessage().deserialize(parsedTitle));
        } catch (Exception e) {
            // Fallback for legacy
            objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', parsedTitle));
        }

        // --- Processing Pipeline ---
        // 1. Static Replacements
        // 2. Fallback Replacements
        // 3. Dynamic Hiding
        // 4. Centering
        // 5. Hide Numbers (Already set on Objective, or via 0 score)

        List<String> finalLines = new ArrayList<>();

        for (String rawLine : lines) {
            // Step 1 & 2: Static & Fallback Replacements
            String processed = applyStaticAndFallback(rawLine, player);

            // Step 3: Dynamic Hiding ({temp-op})
            // If returns null, line is removed (Kill Switch)
            processed = applyDynamicLogic(processed, player);
            if (processed == null)
                continue;

            // Visual Engine: {newline} splitter
            List<String> splitLines = applyNewlineSplit(processed);

            // Add to final list
            for (String split : splitLines) {
                // Final PAPI pass for any remaining placeholders (standard ones)
                String finalPass = split;
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    finalPass = PlaceholderAPI.setPlaceholders(player, finalPass);
                }

                // Step 4: Centering (Inline Literal Logic)
                finalPass = applyCentering(finalPass);

                finalLines.add(finalPass);
            }
        }

        // --- Update Scoreboard Teams ---
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
                break;

            if (lineIndex < finalLines.size()) {
                updateLine(scoreboard, objective, score, finalLines.get(lineIndex));
            } else {
                removeLine(scoreboard, score);
            }
        }
    }

    // --- Step 1 & 2: Static & Fallback ---
    private String applyStaticAndFallback(String text, Player player) {
        // 1. Static Placeholders ({primary_colour})
        String result = applyStaticPlaceholders(text);

        // 2. Replacements Engine (%luckperms_prefix% -> "No Rank")
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

                    // Logic: Strip colors before checking for empty/rules
                    String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', resolved));

                    // If resolved is visually empty "", check for rule ""
                    if (stripped.isEmpty() && rules.containsKey("")) {
                        result = result.replace(papiKey, rules.get(""));
                    } else if (rules.containsKey(stripped)) {
                        result = result.replace(papiKey, rules.get(stripped));
                    } else {
                        // Default logic: just replace with resolved value
                        result = result.replace(papiKey, resolved);
                    }
                }
            }
        }
        return result;
    }

    // --- Step 3: Dynamic Logic ---
    // Returns null if line is removed
    private String applyDynamicLogic(String text, Player player) {
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

        // Check 2: Time Pattern (digits + d/h/m/s or just digits/colons)
        // V10: flexible check active
        if (timeLeft.matches(".*\\d+.*")) { // Contains digits usually means time
            if (dynamicTempOp.containsKey("active")) {
                return text.replace("{temp-op}", dynamicTempOp.get("active").replace("%time%", timeLeft));
            }
            return text.replace("{temp-op}", timeLeft);
        }

        // Check 3: Kill Switch
        // If we reached here, it's neither Relog nor Active Time -> likely "Permanent"
        // or empty or "No Op"
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

    // --- Step 4: Centering (Inline Literal Logic) ---
    private String applyCentering(String text) {
        if (!text.contains("{centre}")) {
            return text;
        }
        String clean = text.replace("{centre}", "");

        // Calculate visible length safely using MiniMessage stripTags
        // This removes ALL tags (<red>, <bold>, etc.) to get raw character count
        String plain = MiniMessage.miniMessage().stripTags(clean);

        int visibleLength = plain.length();
        int padding = (30 - visibleLength) / 2;

        if (padding <= 0) {
            return clean;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }
        sb.append(clean);
        return sb.toString();
    }

    // --- Helpers ---

    private void updateLine(Scoreboard sb, Objective obj, int score, String text) {
        String teamName = "line_" + score;
        Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
            String entry = "§" + Integer.toHexString(score); // Unique entry 0-F
            if (score > 15)
                entry = "§" + score;

            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
        }

        // V10: Use MiniMessage or fallback to legacy
        try {
            team.prefix(MiniMessage.miniMessage().deserialize(text));
        } catch (Exception e) {
            // Fallback
            team.setPrefix(ChatColor.translateAlternateColorCodes('&', text));
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

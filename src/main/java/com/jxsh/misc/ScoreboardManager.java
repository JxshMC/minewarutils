package com.jxsh.misc;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager implements Listener {

    private final JxshMisc plugin;
    // private File configFile; // Removed

    // Config values
    private boolean enabled;
    private String title;
    private List<String> lines;
    private int updateInterval;
    private Map<String, String> customPlaceholders;
    private Map<String, Map<String, String>> conditionalReplacements;

    private BukkitTask updateTask;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    public ScoreboardManager(JxshMisc plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    @SuppressWarnings("unchecked")
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

            conditionalReplacements = new HashMap<>();
            if (config.isSection("Replacements")) {
                dev.dejvokep.boostedyaml.block.implementation.Section replSection = config.getSection("Replacements");
                for (Object key : replSection.getKeys()) {
                    String baseKey = key.toString();
                    if (replSection.isSection(Route.from(key))) {
                        dev.dejvokep.boostedyaml.block.implementation.Section inner = replSection
                                .getSection(Route.from(key));
                        Map<String, String> stringMap = new HashMap<>();
                        for (Object innerKeyObj : inner.getKeys()) {
                            String innerKey = innerKeyObj.toString();
                            String innerVal = inner.getString(Route.from(innerKeyObj), "");
                            stringMap.put(innerKey, innerVal);
                        }
                        conditionalReplacements.put(baseKey, stringMap);
                    }
                }
                plugin.getLogger().info("Scoreboard: Successfully loaded config (Replacements: "
                        + conditionalReplacements.size() + ")");
            }

            if (enabled) {
                startTask();
                // Refresh for online players
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
                MiniMessage.miniMessage().deserialize(title));

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Hide numbers globally for this objective
        objective.numberFormat(NumberFormat.blank());

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
            return; // Should not happen

        // Update title with placeholders (if title has placeholders, though usually
        // static/animated titles need specific handling)
        // Ideally we assume title is static or handled elsewhere for animations, but
        // basic placeholder support:
        String parsedTitle = applyConditionalReplacements(title, player);
        parsedTitle = applyCustomPlaceholders(parsedTitle);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            parsedTitle = PlaceholderAPI.setPlaceholders(player, parsedTitle);
        }
        objective.displayName(MiniMessage.miniMessage().deserialize(parsedTitle));

        // Update lines
        // We use a score-based system where line 0 is at the top (highest score) or
        // bottom?
        // Standard sidebar: Score 15 is top, Score 1 is bottom.
        // Array index 0 should be top.
        // So line 0 -> Score 15 (or size of list)
        // line 1 -> Score 14...

        int scoreValue = lines.size();

        for (String line : lines) {
            if (scoreValue <= 0)
                break;

            String parsedLine = applyConditionalReplacements(line, player);
            parsedLine = applyCustomPlaceholders(parsedLine);
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                parsedLine = PlaceholderAPI.setPlaceholders(player, parsedLine);
            }

            // Using team entries allows for longer lines and anti-flicker, but for modern
            // versions
            // resetting scores matches entries by name.
            // Since we want to hide numbers, we just set the score.
            // However, to update text dynamically without flicker, relying on just scores
            // and resetting them is one way.
            // A better way is using Teams, but let's stick to simple Score setting for
            // simplicity as requested,
            // unless flicker is an issue. With modern clients/server, updating scores is
            // relatively cheap.
            // BUT, to change the TEXT of a line, we actually have to remove the old score
            // and add a new one
            // because the text IS the entry name.

            // To do this simply:
            // Designate "lines" by specific fake player names (or invisible characters) and
            // use Teams to set prefix/suffix?
            // OR just clear all entries and re-add them.

            // For this implementation, I will treat the entry string AS the display text.
            // NOTE: If two lines are identical, they will conflict.
            // To fix duplicates, we can append unseen color codes.

            updateLine(scoreboard, objective, scoreValue, parsedLine);

            scoreValue--;
        }

        // Clean up any extra scores if lines size decreased (unlikely for static config
        // but good practice)
        // This simple implementation relies on fixed size mostly.
    }

    // Helper to genericize line updating using Teams for anti-flicker and support
    // for same-content lines
    private void updateLine(Scoreboard sb, Objective obj, int score, String text) {
        String teamName = "line_" + score;
        org.bukkit.scoreboard.Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
            // Create a unique entry for this score so it persists
            // Use invisible chars or color codes to ensure uniqueness
            String entry = getUniqueEntry(score);
            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
        }

        team.prefix(MiniMessage.miniMessage().deserialize(text));
    }

    private String applyCustomPlaceholders(String text) {
        if (customPlaceholders == null || customPlaceholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private String applyConditionalReplacements(String text, Player player) {
        if (conditionalReplacements == null || conditionalReplacements.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, Map<String, String>> entry : conditionalReplacements.entrySet()) {
            String placeholder = entry.getKey();
            Map<String, String> replacements = entry.getValue();

            // Check if line contains this placeholder
            if (result.contains(placeholder)) {
                // Resolve the placeholder value
                String resolvedValue = "";
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    resolvedValue = PlaceholderAPI.setPlaceholders(player, placeholder);
                }

                // Prepare lookup key
                String lookup = resolvedValue == null ? "" : resolvedValue;

                // Strip legacy colors and MiniMessage tags to check if it's truly "empty"
                String stripped = lookup.replaceAll("§[0-9a-fk-orx]", "")
                        .replaceAll("&[0-9a-fk-orx]", "")
                        .replaceAll("<[^>]*>", "");
                boolean isPlaceholderEmpty = lookup.isEmpty() || stripped.trim().isEmpty()
                        || lookup.equalsIgnoreCase("null");

                // Look for replacement
                if (replacements.containsKey(lookup)) {
                    // Exact match
                    String replacement = replacements.get(lookup);
                    result = result.replace(placeholder, replacement);
                } else if (isPlaceholderEmpty && (replacements.containsKey("") || replacements.containsKey("none"))) {
                    // Specific fallback for empty
                    String keyToUse = replacements.containsKey("") ? "" : "none";
                    String replacement = replacements.get(keyToUse);
                    result = result.replace(placeholder, replacement);
                }
            }
        }
        return result;
    }

    private String getUniqueEntry(int index) {
        // Just use a color code string based on index to ensure it's hidden/unique
        // ChatColor is deprecated in favor of adventure, but for legacy string entries
        // we need legacy colors or just unique strings.
        // We can just use "§[0-9/a-f/r]" combinations or similar.
        // Or simpler: just standard color codes.
        // 0-15 lines.
        return "§" + Integer.toHexString(index);
    }
}

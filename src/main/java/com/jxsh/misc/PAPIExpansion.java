package com.jxsh.misc;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PAPIExpansion extends PlaceholderExpansion {

    private final JxshMisc plugin;

    public PAPIExpansion(JxshMisc plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jxsh";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "minewar";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(org.bukkit.OfflinePlayer player, @NotNull String params) {
        // Resolve nested placeholders (e.g., %minewar_suffix_other%player%%)
        params = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, params);
        // Also resolve bracket placeholders for compatibility ({player_name})
        params = me.clip.placeholderapi.PlaceholderAPI.setBracketPlaceholders(player, params);

        // %minewar_total% - Synced Global Total (Vanish-aware)
        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(plugin.getGlobalTotalCount());
        }

        // %minewar_server_<name>% - Synced Server Count (Vanish-aware)
        if (params.toLowerCase().startsWith("server_")) {
            String serverName = params.substring(7);
            return String.valueOf(plugin.getServerCount(serverName));
        }

        if (params.equalsIgnoreCase("server")) {
            int total = Bukkit.getOnlinePlayers().size();
            int vanishedCount = 0;
            if (plugin.getVanishPacketListener() != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plugin.getVanishPacketListener().isVanished(p.getUniqueId())) {
                        vanishedCount++;
                    }
                }
            }
            return String.valueOf(total - vanishedCount);
        }

        if (player == null)
            return "";

        // %minewar_vanished%
        if (params.equalsIgnoreCase("vanished")) {
            boolean vanished = plugin.getVanishPacketListener() != null
                    && plugin.getVanishPacketListener().isVanished(player.getUniqueId());
            return vanished ? "True" : "False";
        }

        // %minewar_buildmode%
        if (params.equalsIgnoreCase("buildmode")) {
            boolean enabled = plugin.getBuildModeManager() != null
                    && plugin.getBuildModeManager().isBuildModeEnabled(player.getUniqueId());
            return enabled ? "True" : "False";
        }

        // %minewar_buildmode_<player>%
        if (params.toLowerCase().startsWith("buildmode_")) {
            String targetName = params.substring(10);
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                boolean enabled = plugin.getBuildModeManager() != null
                        && plugin.getBuildModeManager().isBuildModeEnabled(target.getUniqueId());
                return enabled ? "True" : "False";
            }
            return "False";
        }

        // Dynamic LuckPerms Placeholders
        // %minewar_prefix_otherJxsh%
        // %minewar_suffix_otherJxsh%
        if (params.toLowerCase().startsWith("prefix_other")) {
            String username = params.substring(12);
            String prefix = plugin.getLuckPermsHook().getPrefix(username);
            return (prefix.isEmpty() ? "" : prefix) + username;
        }
        if (params.toLowerCase().startsWith("suffix_other")) {
            String username = params.substring(12);
            String suffix = plugin.getLuckPermsHook().getSuffix(username);
            return (suffix.isEmpty() ? "" : suffix) + username;
        }

        if (params.equals("tempop_time_left")) {
            com.jxsh.misc.managers.TempOpManager tempOpManager = plugin.getTempOpManager();
            if (tempOpManager == null)
                return "";

            com.jxsh.misc.managers.TempOpManager.OpData data = tempOpManager.getOpData(player.getUniqueId());
            if (data == null)
                return ""; // Or "no-time"? But usually empty if not opped.

            String result = "";
            if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TIME) {
                long remaining = (data.expiration - System.currentTimeMillis()) / 1000;
                if (remaining < 0)
                    remaining = 0;
                result = formatDurationDynamic(remaining);
            } else if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.PERM) {
                result = plugin.getConfigManager().getMessages().getString("commands.tempop.time-left-perm",
                        "Permanent");
            } else {
                result = plugin.getConfigManager().getMessages().getString("commands.tempop.time-left-temp",
                        "Expire-Relog");
            }
            return result;
        }

        return null;
    }

    private String formatDurationDynamic(long totalSeconds) {
        if (totalSeconds <= 0)
            return "Expired";

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String format = plugin.getConfigManager().getMessages().getString("commands.tempop.time-left-format",
                "%days%d, %hours%h, %minutes%m, %seconds%s");

        // Hide 0 units logic:
        // We can't strictly use the format string if we are hiding units, because the
        // separators (commas) might be left dangling.
        // But the user said: "Logic: If a unit is 0, hide it."
        // And "time-left-format: %days%d, %hours%h, %minutes%m, %seconds%s"
        // This suggests we should build it dynamically based on the PRESENCE of units.
        // OR we replace 0 units with empty string?
        // "0d, " -> ""? That leaves ", ".
        // Let's build it dynamically using the suffix from the config?
        // Actually, typically "Hide 0 units" means building a list of non-zero parts.

        java.util.List<String> parts = new java.util.ArrayList<>();
        if (days > 0)
            parts.add(days + "d");
        if (hours > 0)
            parts.add(hours + "h");
        if (minutes > 0)
            parts.add(minutes + "m");
        if (seconds > 0)
            parts.add(seconds + "s");

        if (parts.isEmpty())
            return "0s"; // Or "Expired"?

        return String.join(", ", parts);
    }
}

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
                return plugin.getConfigManager().getMessages().getString("commands.tempop.no-time", "N/A");

            com.jxsh.misc.managers.TempOpManager.OpData data = tempOpManager.getOpData(player.getUniqueId());

            // Strict Priority Logic

            // Priority 4 (Fallback/Not Op in our system)
            if (data == null) {
                return plugin.getConfigManager().getMessages().getString("commands.tempop.no-time", "N/A");
            }

            // Priority 1 (Permanent)
            if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.PERM) {
                return plugin.getConfigManager().getMessages().getString("commands.tempop.time-left-perm", "Permanent");
            }

            // Priority 2 (Session/Relog)
            // Defined as OpType.TEMP or OpType.TIME with 0 expiration (sanity check, though
            // TIME should have expiration)
            if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TEMP
                    || (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TIME && data.expiration == 0)) {
                return plugin.getConfigManager().getMessages().getString("commands.tempop.time-left-temp",
                        "Expire-Relog");
            }

            // Priority 3 (Timed)
            if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TIME) {
                long remaining = (data.expiration - System.currentTimeMillis()) / 1000;
                if (remaining < 0)
                    remaining = 0;

                String format = plugin.getConfigManager().getMessages().getString("commands.tempop.time-left-format",
                        "%days%d, %hours%h, %minutes%m, %seconds%s");
                return formatDurationConfigurable(remaining, format);
            }

            return plugin.getConfigManager().getMessages().getString("commands.tempop.no-time", "N/A");
        }

        return null;
    }

    private String formatDurationConfigurable(long totalSeconds, String format) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // Logic: If a unit is 0, we might want to hide it if the user wants.
        // But the format string is %days%d etc.
        // If the format contains explicit keys, we replace them.

        // Simple replacement
        String result = format
                .replace("%days%", String.valueOf(days))
                .replace("%hours%", String.valueOf(hours))
                .replace("%minutes%", String.valueOf(minutes))
                .replace("%seconds%", String.valueOf(seconds));

        return result;
    }
}

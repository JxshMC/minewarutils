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

        return null;
    }
}

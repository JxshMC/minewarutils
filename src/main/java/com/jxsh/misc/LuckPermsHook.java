package com.jxsh.misc;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;

public class LuckPermsHook {

    private LuckPerms luckPerms;

    public LuckPermsHook(JxshMisc plugin) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            this.luckPerms = LuckPermsProvider.get();
        }
    }

    public String getPrefix(String username) {
        if (luckPerms == null)
            return "";
        // Try online first
        User user = luckPerms.getUserManager().getUser(username);
        if (user == null) {
            // Try offline lookup
            try {
                java.util.UUID uuid = luckPerms.getUserManager().lookupUniqueId(username).get();
                if (uuid != null) {
                    user = luckPerms.getUserManager().loadUser(uuid).get();
                }
            } catch (Exception e) {
                // Ignore or log
            }
        }

        if (user == null)
            return "";
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix) : "";
    }

    public String getSuffix(String username) {
        if (luckPerms == null)
            return "";
        // Try online first
        User user = luckPerms.getUserManager().getUser(username);
        if (user == null) {
            // Try offline lookup
            try {
                java.util.UUID uuid = luckPerms.getUserManager().lookupUniqueId(username).get();
                if (uuid != null) {
                    user = luckPerms.getUserManager().loadUser(uuid).get();
                }
            } catch (Exception e) {
                // Ignore or log
            }
        }

        if (user == null)
            return "";
        String suffix = user.getCachedData().getMetaData().getSuffix();
        return suffix != null ? org.bukkit.ChatColor.translateAlternateColorCodes('&', suffix) : "";
    }

    public int getWeight(String username) {
        if (luckPerms == null)
            return 0;
        // Try online first
        User user = luckPerms.getUserManager().getUser(username);
        if (user == null) {
            // Try offline lookup
            try {
                java.util.UUID uuid = luckPerms.getUserManager().lookupUniqueId(username).get();
                if (uuid != null) {
                    user = luckPerms.getUserManager().loadUser(uuid).get();
                }
            } catch (Exception e) {
                // Ignore or log
            }
        }

        if (user == null)
            return 0;

        String primaryGroup = user.getPrimaryGroup();
        net.luckperms.api.model.group.Group group = luckPerms.getGroupManager().getGroup(primaryGroup);
        return group != null ? group.getWeight().orElse(0) : 0;
    }

    /**
     * Parses custom placeholders: %prefix_other%(username) and
     * %suffix_other%(username)
     * Returns "Prefix Username" or "Suffix Username" respectively.
     */
    public String parseDynamicPlaceholders(String text) {
        if (text == null || text.isEmpty())
            return text;

        // Regex to match %prefix_other%username and %suffix_other%username
        // Group 1: prefix/suffix, Group 2: username (Minecraft usernames are 3-16
        // chars, alphanumeric/underscore)
        // Using \b or a lookbehind/ahead might be tricky in chat strings, so we use a
        // specific range for the name.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("%(prefix|suffix)_other%([a-zA-Z0-9_]{3,16})",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String type = matcher.group(1); // prefix or suffix
            String username = matcher.group(2); // the name immediately following

            String result;
            if (type.equalsIgnoreCase("prefix")) {
                String prefix = getPrefix(username);
                result = (prefix.isEmpty() ? "" : prefix) + username;
            } else {
                String suffix = getSuffix(username);
                result = (suffix.isEmpty() ? "" : suffix) + username;
            }
            sb.append(result);
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }
}

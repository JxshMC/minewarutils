package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MinewarUtilsCommand extends BaseCommand {

    public MinewarUtilsCommand(JxshMisc plugin) {
        super(plugin, "minewarutils", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Display only the header message
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("minewarutils-header"),
                    (sender instanceof Player ? (Player) sender : null)));
            return;
        }

        String sub = args[0].toLowerCase();

        // Dynamic Sub-commands
        // We know the main key is "minewarutils"
        // We need to match 'sub' against configured names/aliases

        // Helper to check match
        if (matchesSubCommand("reload", sub)) {
            handleReload(sender);
            return;
        }
        if (matchesSubCommand("sneak", sub)) {
            handleSneak(sender, args);
            return;
        }
        if (matchesSubCommand("world", sub)) {
            handleWorld(sender, args);
            return;
        }
        if (matchesSubCommand("help", sub)) {
            handleHelp(sender, args);
            return;
        }

        // Support /mu <world> <setting> <value>
        World worldFirst = Bukkit.getWorld(args[0]);
        if (worldFirst != null && args.length >= 3) {
            handleWorldRequest(sender, worldFirst, args, 1);
            return;
        }

        sender.sendMessage(plugin.parseText(
                plugin.getConfigManager().getMessages().getString("commands.error.unknown-subcommand"),
                (sender instanceof Player ? (Player) sender : null)));
    }

    private boolean matchesSubCommand(String internalInfo, String args0) {
        String mainKey = "minewarutils";
        String confName = plugin.getConfigManager().getSubCommandName(mainKey, internalInfo);
        if (confName.equalsIgnoreCase(args0))
            return true;
        if (confName.startsWith("/") && confName.substring(1).equalsIgnoreCase(args0))
            return true;

        List<String> aliases = plugin.getConfigManager().getSubCommandAliases(mainKey, internalInfo);
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(args0))
                return true;
        }
        return false;
    }

    private void handleReload(CommandSender sender) {
        // Permission checked in BaseCommand by permissionKey
        // But /mu reload has its own permission key?
        // BaseCommand constructor uses "minewarutils" for permissionKey

        if (!plugin.hasPermission(sender, "reload")) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.error.no-permission"),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        plugin.fullReload();
        sender.sendMessage(plugin.parseText(
                plugin.getConfigManager().getMessages().getString("commands.reload",
                        "<green>Plugin successfully rebooted and files verified."),
                (sender instanceof Player ? (Player) sender : null)));
    }

    private void handleSneak(CommandSender sender, String[] args) {
        if (!plugin.hasPermission(sender, "sneak")) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.error.no-permission"),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.error.usage"),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        String targetName;
        if (args.length > 2) {
            targetName = args[2];
        } else {
            if (sender instanceof Player) {
                targetName = sender.getName();
            } else {
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("commands.error.no-console"), null));
                return;
            }
        }

        List<String> currentUsers = plugin.getBoostedConfig().getStringList("enabled-users");
        if (args[1].equalsIgnoreCase("enable")) {
            if (!currentUsers.contains(targetName)) {
                currentUsers.add(targetName);
                plugin.getBoostedConfig().set("enabled-users", currentUsers);
                plugin.saveConfig();
                plugin.loadConfigValues();
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.sneak.enabled").replace("%target%",
                                targetName),
                        sender instanceof Player ? (Player) sender : null));
            } else {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.sneak.already-enabled").replace(
                                "%target%",
                                targetName),
                        sender instanceof Player ? (Player) sender : null));
            }
        } else if (args[1].equalsIgnoreCase("disable")) {
            if (currentUsers.contains(targetName)) {
                currentUsers.remove(targetName);
                plugin.getBoostedConfig().set("enabled-users", currentUsers);
                plugin.saveConfig();
                plugin.loadConfigValues();
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.sneak.disabled").replace("%target%",
                                targetName),
                        sender instanceof Player ? (Player) sender : null));
            } else {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.sneak.not-enabled").replace(
                                "%target%",
                                targetName),
                        sender instanceof Player ? (Player) sender : null));
            }
        }
    }

    private void handleWorld(CommandSender sender, String[] args) {
        if (!plugin.hasPermission(sender, "world-flags")) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.error.no-permission"),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages().getString("world-usage"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        World targetWorld = Bukkit.getWorld(args[1]);
        if (targetWorld != null) {
            handleWorldRequest(sender, targetWorld, args, 2);
        } else {
            if (sender instanceof Player player) {
                handleWorldRequest(sender, player.getWorld(), args, 1);
            } else {
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("world-console-specify"), null));
            }
        }
    }

    private void handleWorldRequest(CommandSender sender, World world, String[] args, int startIndex) {
        if (args.length <= startIndex)
            return;
        String possibleSetting = args[startIndex];
        int finalStartIndex = startIndex;
        World targetWorld = world;

        List<String> settingsList = Arrays.asList("destroy", "place", "pvp", "invincible", "spawnmobs", "nomobs",
                "antivoid", "blockdecay", "nodecay", "0tick");

        if (settingsList.contains(possibleSetting.toLowerCase())) {
            // Already using the correct startIndex
        } else {
            // Try to see if it's a world name (redundant but safe)
            World w = Bukkit.getWorld(possibleSetting);
            if (w != null) {
                targetWorld = w;
                finalStartIndex = startIndex + 1;
            } else {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("world-unknown-setting")
                                .replace("%setting%", possibleSetting),
                        (sender instanceof Player ? (Player) sender : null)));
                return;
            }
        }

        if (args.length <= finalStartIndex + 1) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("world-missing-value"),
                            (sender instanceof Player ? (Player) sender : null)));
            return;
        }

        String setting = args[finalStartIndex];
        String value = args[finalStartIndex + 1];

        if (setting.equalsIgnoreCase("antivoid") && args.length > finalStartIndex + 2) {
            String levelValue = args[finalStartIndex + 2];
            plugin.getWorldManager().setWorldSetting(targetWorld, "antivoid", value,
                    sender instanceof Player ? (Player) sender : null);
            plugin.getWorldManager().setWorldSetting(targetWorld, "antivoidlevel", levelValue,
                    sender instanceof Player ? (Player) sender : null);
        } else {
            plugin.getWorldManager().setWorldSetting(targetWorld, setting, value,
                    sender instanceof Player ? (Player) sender : null);
        }
    }

    private void handleHelp(CommandSender sender, String[] args) {
        // Parse page number
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        plugin.getHelpManager().showHelp(sender, page);
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new java.util.ArrayList<>();
            // Add world names first
            suggestions.addAll(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()));

            // Add dynamic sub-commands
            String mainKey = "minewarutils";
            suggestions.add(plugin.getConfigManager().getSubCommandName(mainKey, "reload"));
            suggestions.add(plugin.getConfigManager().getSubCommandName(mainKey, "sneak"));
            suggestions.add(plugin.getConfigManager().getSubCommandName(mainKey, "world"));
            suggestions.add(plugin.getConfigManager().getSubCommandName(mainKey, "help"));

            // Add aliases for them?
            // Usually tab complete only shows main command, but if aliases are popular we
            // can add them.
            // User requested: "The full "reload" command should now be "/mu reload" or "/mu
            // rl""
            suggestions.addAll(plugin.getConfigManager().getSubCommandAliases(mainKey, "reload"));
            suggestions.addAll(plugin.getConfigManager().getSubCommandAliases(mainKey, "sneak"));
            suggestions.addAll(plugin.getConfigManager().getSubCommandAliases(mainKey, "world"));
            suggestions.addAll(plugin.getConfigManager().getSubCommandAliases(mainKey, "help"));

            return filter(suggestions, args);
        }
        if (args.length > 1) {
            String sub = args[0].toLowerCase();

            if (matchesSubCommand("sneak", sub)) {
                if (args.length == 2)
                    return filter(Arrays.asList("enable", "disable"), args);
                if (args.length == 3)
                    return filter(getOnlinePlayerNames(sender), args);
            }
            if (matchesSubCommand("world", sub)) {
                return tabCompleteWorld(sender, args);
            }

            // Logic for /mu <world> ...
            World possibleWorld = Bukkit.getWorld(args[0]);
            if (possibleWorld != null) {
                return tabCompleteWorld(sender, args);
            }
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteWorld(CommandSender sender, String[] args) {
        List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
        List<String> settings = Arrays.asList("destroy", "place", "pvp", "invincible", "spawnmobs", "nomobs",
                "antivoid", "blockdecay", "nodecay", "0tick");
        List<String> bools = Arrays.asList("true", "false");

        int offset = args[0].equalsIgnoreCase("world") ? 1 : 0;
        int adjustedLength = args.length - offset;

        // After "/mu world" - suggest world names
        if (adjustedLength == 1) {
            return offset == 1 ? worlds : settings;
        }
        // After world name is entered - suggest settings only
        if (adjustedLength == 2) {
            return settings;
        }
        // After setting is entered - suggest true/false
        if (adjustedLength == 3) {
            return bools;
        }
        // For antivoid level, leave empty (user can type any number)
        return Collections.emptyList();
    }
}

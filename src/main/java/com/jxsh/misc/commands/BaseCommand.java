package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final JxshMisc plugin;
    private final String permissionKey;
    private final boolean playerOnly;

    public BaseCommand(JxshMisc plugin, String permissionKey, boolean playerOnly) {
        this.plugin = plugin;
        this.permissionKey = permissionKey;
        this.playerOnly = playerOnly;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        // Config-Ready Guard
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getConfig() == null) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("internal-error-config", "<red>Internal Error: Configuration not loaded!"), null));
            return true;
        }

        // 1. Structured Permission Guard
        if (!plugin.hasPermission(sender, permissionKey)) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return true;
        }

        // 2. Player-Only Check
        if (playerOnly && !(sender instanceof Player)) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.error.no-console"), null));
            return true;
        }

        execute(sender, args, label);
        return true;
    }

    public abstract void execute(CommandSender sender, String[] args, String label);

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        // 1. Permission Check
        if (!plugin.getConfigManager().getConfig().getBoolean("features." + permissionKey, true)) {
            return Collections.emptyList();
        }
        if (!plugin.hasPermission(sender, permissionKey)) {
            return Collections.emptyList();
        }

        // 2. Delegate to custom logic
        List<String> completions = tabComplete(sender, args);
        if (completions != null) {
            return filter(completions, args);
        }

        // 3. Default "Smart" Logic
        if (args.length > 0) {
            String lastArg = args[args.length - 1];
            // If the last argument looks like a player name (or we are at pos 1 for most
            // commands), return players
            // Heuristic: If we are at index 0 (length 1), always return players unless
            // overridden.
            if (args.length == 1) {
                return filter(getOnlinePlayerNames(sender), args);
            }
            // For index > 1, some commands might take players too (like /give item amount
            // player),
            // but we can't guess easily without schema.
            // Subclasses should override tabComplete for specific non-player args.
            // If we return null from overriding tabComplete, we fall back to this or empty?
            // "If no logic is defined... prioritize sender's own name..."
        }

        return Collections.emptyList();
    }

    /**
     * Subclasses can override this to provide specific tab completions.
     * Return null to use default logic (Online Players for arg 1).
     * Return empty list to show nothing.
     */
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }

    protected List<String> getOnlinePlayerNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        // Sender first
        if (sender instanceof Player) {
            names.add(sender.getName());
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(p.getUniqueId()))
                continue;
            names.add(p.getName());
        }
        return names;
    }

    protected List<String> filter(List<String> candidates, String[] args) {
        if (args.length == 0)
            return candidates;
        String last = args[args.length - 1].toLowerCase();
        return candidates.stream()
                .filter(s -> s.toLowerCase().startsWith(last))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a target player from arguments.
     * If args has an element at index, attempts to find that player.
     * If no arg at index, returns the sender if they are a player.
     * Throws IllegalArgumentException if target cannot be found or if sender is
     * console and no arg provided.
     */
    protected Player resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player target = Bukkit.getPlayer(args[index]);
            if (target == null) {
                // Check if it's a valid offline player if needed, but usually for commands we
                // want online players
                // Subclasses can implement offline logic if specific command needs it
                String msg = plugin.getConfigManager().getMessages().getString("commands.error.invalid-player");
                sender.sendMessage(plugin.parseText(msg.replace("%target%", args[index]),
                        sender instanceof Player ? (Player) sender : null));
                return null;
            }
            return target;
        } else {
            if (sender instanceof Player player) {
                return player;
            } else {
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.error.no-console"),
                                null));
                return null;
            }
        }
    }

    /**
     * Checks if the provided argument matches a sub-command's name or any of its
     * aliases.
     * 
     * @param arg     The argument to check (e.g. args[0])
     * @param mainKey The internal key of the main command (e.g. "warps")
     * @param subKey  The internal key of the sub-command (e.g. "set")
     * @return true if matches
     */
    protected boolean checkSubCommand(String arg, String mainKey, String subKey) {
        // 1. Check strict name
        String configName = plugin.getConfigManager().getSubCommandName(mainKey, subKey);
        if (arg.equalsIgnoreCase(configName))
            return true; // Matches configured name (e.g. "set")

        // 2. Check aliases
        List<String> aliases = plugin.getConfigManager().getSubCommandAliases(mainKey, subKey);
        for (String alias : aliases) {
            if (arg.equalsIgnoreCase(alias))
                return true;
        }

        return false;
    }
}

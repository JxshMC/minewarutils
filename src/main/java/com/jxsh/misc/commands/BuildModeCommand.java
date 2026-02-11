package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.BuildModeManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildModeCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final BuildModeManager buildModeManager;

    public BuildModeCommand(JxshMisc plugin, BuildModeManager buildModeManager) {
        super(plugin, "buildmode", true);
        this.plugin = plugin;
        this.buildModeManager = buildModeManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Permission and console checks handled by BaseCommand

        Player target;

        if (args.length > 0) {
            // Check permission for others
            if (!plugin.hasPermission(sender, "buildmode-others")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                String errorMsg = plugin.getConfigManager().getMessages()
                        .getString("commands.error.invalid-player", "<red>Invalid player.");
                errorMsg = errorMsg.replace("%target%", args[0]);
                sender.sendMessage(plugin.parseText(errorMsg, sender instanceof Player ? (Player) sender : null));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.no-console",
                                "<red>Console must specify a player name."),
                        null));
                return;
            }
            target = (Player) sender;
        }

        boolean newState = !buildModeManager.isBuildModeEnabled(target.getUniqueId());
        buildModeManager.setBuildMode(target.getUniqueId(), newState);

        if (newState) {
            // Enable: Set to Creative mode
            target.setGameMode(org.bukkit.GameMode.CREATIVE);
            target.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("buildmode.enabled"), target));
            if (!target.equals(sender)) {
                String msg = plugin.getConfigManager().getMessages().getString("buildmode.enabled-other",
                        "<green>Enabled build mode for %target%.");
                msg = msg.replace("%target%", target.getName());
                sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
            }
        } else {
            // Disable: Set to Survival and teleport to spawn
            target.setGameMode(org.bukkit.GameMode.SURVIVAL);

            // Teleport to spawn
            if (plugin.getSpawnManager() != null) {
                plugin.getSpawnManager().teleportToSpawn(target);
            }

            target.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("buildmode.disabled"), target));
            if (!target.equals(sender)) {
                String msg = plugin.getConfigManager().getMessages().getString("buildmode.disabled-other",
                        "<green>Disabled build mode for %target%.");
                msg = msg.replace("%target%", target.getName());
                sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
            }
        }
    }
}

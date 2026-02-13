package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.BuildModeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildModeAdminCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final BuildModeManager buildModeManager;

    public BuildModeAdminCommand(JxshMisc plugin, BuildModeManager buildModeManager) {
        super(plugin, "bmadmin", true);
        this.plugin = plugin;
        this.buildModeManager = buildModeManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        // Permission check handled by BaseCommand ("minewar.buildmode.admin")

        Player target;

        if (args.length > 0) {
            // Target another player
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                String errorMsg = plugin.getConfigManager().getMessages().getString("invalid-player",
                        "<red>Invalid player.");
                errorMsg = errorMsg.replace("%target%", args[0]);
                sender.sendMessage(plugin.parseText(errorMsg, sender instanceof Player ? (Player) sender : null));
                return;
            }
        } else {
            // Target self
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.parseText("<red>Console must specify a player.", null));
                return;
            }
            target = (Player) sender;
        }

        // Ensure they are in build mode first if enabling admin
        if (!buildModeManager.isAdminModeEnabled(target.getUniqueId())
                && !buildModeManager.isBuildModeEnabled(target.getUniqueId())) {
            buildModeManager.setBuildMode(target.getUniqueId(), true); // Force enable build mode
        }

        boolean newState = !buildModeManager.isAdminModeEnabled(target.getUniqueId());
        buildModeManager.setAdminMode(target.getUniqueId(), newState);

        if (newState) {
            target.sendMessage(
                    plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("buildmode.admin-enabled"),
                            target));
            if (!target.equals(sender)) {
                String msg = plugin.getConfigManager().getMessages().getString("buildmode.admin-enabled-other",
                        "<green>Enabled admin mode for %target%.");
                msg = msg.replace("%target%", target.getName());
                sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
            }
        } else {
            target.sendMessage(
                    plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("buildmode.admin-disabled"),
                            target));
            if (!target.equals(sender)) {
                String msg = plugin.getConfigManager().getMessages().getString("buildmode.admin-disabled-other",
                        "<green>Disabled admin mode for %target%.");
                msg = msg.replace("%target%", target.getName());
                sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
            }
        }
    }
}

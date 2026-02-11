package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.TempOpManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.List;
import java.util.Collections;

public class DeopCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final TempOpManager tempOpManager;

    public DeopCommand(JxshMisc plugin, TempOpManager tempOpManager) {
        super(plugin, "tempop-remove", true);
        this.plugin = plugin;
        this.tempOpManager = tempOpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage(plugin
                    .parseText("<red>Usage: /deop <player>", player));
            return;
        }

        String targetName = args[0];
        // Permission check for deop specifically
        if (!plugin.hasPermission(player, "tempop-remove")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.no-permission"), player));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = null;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (off.hasPlayedBefore() || off.isOp()) {
                targetUUID = off.getUniqueId();
            }
        }

        if (targetUUID == null || !tempOpManager.isTempOpp(targetUUID)) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (off.isOp()) {
                off.setOp(false);
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("commands.tempop.revoke-sender", "<green>Op revoked from %target%.")
                        .replace("%target%", targetName), player)); // Fixed sender type
                return;
            }

            sender.sendMessage(plugin.parseText("<red>That player is not temporarily opped.", player));
            return;
        }

        tempOpManager.revokeOp(targetUUID);
        sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                .getString("commands.tempop.revoke-sender", "<green>TempOp revoked from %target%.")
                .replace("%target%", targetName), player));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayerNames(sender);
        }
        return Collections.emptyList();
    }
}

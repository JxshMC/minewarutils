package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.TempOpManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DeopCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final TempOpManager tempOpManager;

    public DeopCommand(JxshMisc plugin, TempOpManager tempOpManager) {
        super(plugin, "deop", true);
        this.plugin = plugin;
        this.tempOpManager = tempOpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minewar.deop") && !sender.isOp()) { // Fallback perm check
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.no-permission"), null));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.parseText("<red>Usage: /deop <player>", null));
            return;
        }

        String targetName = args[0];

        // Try online
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

        if (targetUUID == null) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.error.invalid-player").replace("%target%", targetName), null));
            return;
        }

        // Logic: If in temp op map, revoke it. If vanilla op, deop.
        if (tempOpManager.isTempOpp(targetUUID)) {
            tempOpManager.revokeOp(targetUUID);
        } else {
            // Vanilla DeOp
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetUUID);
            if (off.isOp()) {
                off.setOp(false);
            } else {
                sender.sendMessage(plugin.parseText("<red>That player is not opped.", null));
                return;
            }
        }

        sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                .getString("commands.tempop.revoke-sender", "<green>Op revoked from %target%.")
                .replace("%target%", targetName), null));
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest all OPs? Or just online players?
            return getOnlinePlayerNames(sender);
        }
        return java.util.Collections.emptyList();
    }
}

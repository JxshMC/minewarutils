package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.TempOpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import java.util.UUID;

public class TempOpCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final TempOpManager tempOpManager;

    public TempOpCommand(JxshMisc plugin, TempOpManager tempOpManager) {
        super(plugin, "tempop", true);
        this.plugin = plugin;
        this.tempOpManager = tempOpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.tempop.usage"), player));
            return;
        }

        // Handle "remove" subcommand explicitly: /tempop remove <player>
        if (args[0].equalsIgnoreCase("remove")) {
            if (!plugin.hasPermission(player, "tempop-remove")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.no-permission"), player));
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.parseText("<red>Usage: /tempop remove <player>", player));
                return;
            }
            handleRemove(player, args[1]);
            return;
        }

        // Handle Grant: /tempop <player> [perm|time]
        if (!plugin.hasPermission(player, "tempop-grant")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.no-permission"), player));
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.error.invalid-player").replace("%target%", targetName), player));
            return;
        }

        if (target == player) {
            sender.sendMessage(plugin.parseText("<red>You cannot Temp-OP yourself.", player));
            return;
        }

        if (target.isOp()) {
            if (!tempOpManager.isTempOpp(target.getUniqueId())) {
                String msg = plugin.getConfigManager().getMessages().getString("commands.op-manager.already-op")
                        .replace("%target%", target.getName());
                sender.sendMessage(plugin.parseText(msg, player));
                return;
            }
        }

        TempOpManager.OpType type = TempOpManager.OpType.TEMP;
        long duration = 0;

        if (args.length > 1) {
            String arg1 = args[1].toLowerCase();
            if (arg1.equals("perm")) {
                type = TempOpManager.OpType.PERM;
            } else {
                type = TempOpManager.OpType.TIME;
                try {
                    duration = parseDuration(arg1);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin
                            .parseText("<red>Invalid duration format. Use seconds (e.g. 300) or 1h, 1d.", player));
                    return;
                }
            }
        }

        tempOpManager.grantOp(player, target, type, duration);
    }

    private void handleRemove(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID = null; // We need UUID. If offline, we might need to lookup or iterate activeOps?

        // Try online
        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Try looking up in activeOps by name? Not stored by name.
            // Try offline player
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (off.hasPlayedBefore() || off.isOp()) { // isOp check for offline perm ops?
                targetUUID = off.getUniqueId();
            }
        }

        if (targetUUID == null || !tempOpManager.isTempOpp(targetUUID)) {
            // If they are vanilla OP but not in temp map, do we allow deop?
            // Standard /deop works on vanilla ops.
            // If this command REPLACES /deop logic, we should allow deopping vanilla ops
            // too?
            // User said: "alias for the /tempop remove that's /deop".
            // Implies it should just run tempop remove logic.
            // But if I deop a vanilla op using /deop, I expect it to work.
            // Let's assume if not Key in map, but isOp, we just setOp(false).
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (off.isOp()) {
                off.setOp(false);
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("commands.tempop.revoke-sender", "<green>Op revoked from %target%.") // Reuse or
                                                                                                        // generic
                        .replace("%target%", targetName), sender));
                return;
            }

            sender.sendMessage(plugin.parseText("<red>That player is not opped.", sender));
            return;
        }

        tempOpManager.revokeOp(targetUUID);
        sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                .getString("commands.tempop.revoke-sender", "<green>TempOp revoked from %target%.")
                .replace("%target%", targetName), sender));
    }

    private long parseDuration(String input) {
        if (input.matches("\\d+")) {
            return Long.parseLong(input); // Default to seconds
        }

        long totalSeconds = 0;
        // Simple regex or iteration.
        // Let's support simple single unit for now as specificed in prompt "1d", "1h".
        // Regex for <number><unit>
        if (input.matches("\\d+[smhd]")) {
            long val = Long.parseLong(input.substring(0, input.length() - 1));
            char unit = input.charAt(input.length() - 1);
            switch (unit) {
                case 's':
                    totalSeconds = val;
                    break;
                case 'm':
                    totalSeconds = val * 60;
                    break;
                case 'h':
                    totalSeconds = val * 3600;
                    break;
                case 'd':
                    totalSeconds = val * 86400;
                    break;
            }
            return totalSeconds;
        }
        throw new IllegalArgumentException("Invalid format");
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Suggest players + "remove"
            java.util.List<String> list = getOnlinePlayerNames(sender);
            list.add("remove");
            return list;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                return getOnlinePlayerNames(sender); // Players to remove
            }
            // Suggest types
            return java.util.Arrays.asList("perm", "60", "300", "1h", "1d");
        }
        return java.util.Collections.emptyList();
    }
}

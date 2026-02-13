package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.TempOpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempOpCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final TempOpManager tempOpManager;

    public TempOpCommand(JxshMisc plugin, TempOpManager tempOpManager) {
        super(plugin, "tempop", true);
        this.plugin = plugin;
        this.tempOpManager = tempOpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Player player = (Player) sender;

        if (plugin.getConfigManager() == null || plugin.getConfigManager().getMessages() == null) {
            sender.sendMessage("§cPlugin loading... please wait.");
            return;
        }

        if (args.length < 1) {
            String msg = plugin.getConfigManager().getMessages().getString("commands.tempop.usage",
                    "§cUsage: /tempop <player> [duration]");
            sender.sendMessage(plugin.parseText(msg, player));
            return;
        }

        // Handle "remove" subcommand explicitly: /tempop remove <player>
        // But plan says "delete /tempop remove. Replace all removal logic with a
        // customizable /deop command."
        // So we should remove this? The prompt says "Removal: Delete /tempop remove."
        // Okay, I will remove strict handling here, but maybe keep it as legacy if user
        // wants?
        // No, strict plan: "Delete /tempop remove".
        // Use /deop instead.

        // Command: /op <player> [perm|time]
        // If the user aliases /op to /tempop, then args[0] is player.

        String targetName = args[0];
        // Check if target is "remove" and warn? Or just treat "remove" as a player
        // name?
        if (targetName.equalsIgnoreCase("remove")) {
            sender.sendMessage(plugin.parseText("<red>Use /deop <player> to remove OP.", player));
            return;
        }

        if (!plugin.hasPermission(player, "tempop-grant")) {
            String msg = plugin.getConfigManager().getMessages().getString("commands.error.no-permission",
                    "§cNo permission.");
            sender.sendMessage(plugin.parseText(msg, player));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            String msg = plugin.getConfigManager().getMessages().getString("commands.error.invalid-player",
                    "§cPlayer not found.");
            sender.sendMessage(plugin.parseText(msg.replace("%target%", targetName), player));
            return;
        }

        if (target == player) {
            sender.sendMessage(plugin.parseText("<red>You cannot Temp-OP yourself.", player));
            return;
        }

        // Logic:
        // 1. /op <player> (No args) -> Relog-Only (TEMP)
        // 2. /op <player> <duration> -> Timed (TIME)
        // 3. /op <player> perm -> PERM

        TempOpManager.OpType type = TempOpManager.OpType.TEMP; // Default relog-only
        long duration = 0;

        if (args.length > 1) {
            String arg1 = args[1].toLowerCase();
            if (arg1.equals("perm") || arg1.equals("permanent")) {
                type = TempOpManager.OpType.PERM;
            } else {
                type = TempOpManager.OpType.TIME;
                try {
                    duration = parseDuration(arg1);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin
                            .parseText("<red>Invalid duration format. Example: 1d2h, 30m, 60s.", player));
                    return;
                }
            }
        }

        // Check if already OP
        if (target.isOp()) {
            // If they are vanilla OP (not in our system), or in our system.
            // If in our system, we might want to overwrite?
            // Prompt says: "If the Admin or the Target logs out, de-op the target
            // immediately."
            // So we just overwrite/update.
            // If vanilla OP, we might be "downgrading" them to temp op.
            // That's fine.
        }

        tempOpManager.grantOp(player, target, type, duration);
    }

    private long parseDuration(String input) {
        if (input.matches("\\d+")) {
            return Long.parseLong(input); // Default to seconds
        }

        long totalSeconds = 0;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)([wdhms])");
        java.util.regex.Matcher m = p.matcher(input);

        while (m.find()) {
            long val = Long.parseLong(m.group(1));
            String unit = m.group(2);
            switch (unit) {
                case "w":
                    totalSeconds += val * 7 * 86400;
                    break;
                case "d":
                    totalSeconds += val * 86400;
                    break;
                case "h":
                    totalSeconds += val * 3600;
                    break;
                case "m":
                    totalSeconds += val * 60;
                    break;
                case "s":
                    totalSeconds += val;
                    break;
            }
        }

        if (totalSeconds == 0)
            throw new IllegalArgumentException("Invalid format");
        return totalSeconds;
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayerNames(sender);
        }
        if (args.length == 2) {
            return java.util.Arrays.asList("perm", "1d", "12h", "30m");
        }
        return java.util.Collections.emptyList();
    }
}

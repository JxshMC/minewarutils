package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.TempOpManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class OpsCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final TempOpManager tempOpManager;

    public OpsCommand(JxshMisc plugin, TempOpManager tempOpManager) {
        super(plugin, "ops", true);
        this.plugin = plugin;
        this.tempOpManager = tempOpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!plugin.hasPermission((Player) sender, "ops.list")) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.error.no-permission"),
                            (Player) sender));
            return;
        }

        Set<OfflinePlayer> allOps = Bukkit.getOperators();
        Set<UUID> tempOps = tempOpManager.getTempOps();

        List<OfflinePlayer> permOpsList = new ArrayList<>();
        List<OfflinePlayer> tempOpsList = new ArrayList<>();

        for (OfflinePlayer op : allOps) {
            if (tempOps.contains(op.getUniqueId())) {
                tempOpsList.add(op);
            } else {
                permOpsList.add(op);
            }
        }

        // Sorting Logic: Weight (descending), then Name (ascending)
        Comparator<OfflinePlayer> comparator = (p1, p2) -> {
            int weight1 = 0;
            int weight2 = 0;

            if (plugin.getLuckPermsHook() != null) {
                weight1 = plugin.getLuckPermsHook().getWeight(p1.getName());
                weight2 = plugin.getLuckPermsHook().getWeight(p2.getName());
            }

            if (weight1 != weight2) {
                return Integer.compare(weight2, weight1); // Descending
            }

            String n1 = p1.getName();
            String n2 = p2.getName();
            if (n1 == null)
                n1 = "";
            if (n2 == null)
                n2 = "";

            return n1.compareToIgnoreCase(n2);
        };

        permOpsList.sort(comparator);
        tempOpsList.sort(comparator);

        // Formatting
        // Retrieve formats
        String permFormat = plugin.getConfigManager().getMessages()
                .getString("commands.op-manager.ops-list-format.name-format.perm-ops", "%player%");
        String tempFormat = plugin.getConfigManager().getMessages()
                .getString("commands.op-manager.ops-list-format.name-format.temp-ops", "%player%");

        List<String> permFormatted = new ArrayList<>();
        for (OfflinePlayer p : permOpsList) {
            String name = p.getName() != null ? p.getName() : "Unknown";
            permFormatted.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .serialize(plugin.parseText(permFormat.replace("%player%", name), p)));
        }

        List<String> tempFormatted = new ArrayList<>();
        for (OfflinePlayer p : tempOpsList) {
            String name = p.getName() != null ? p.getName() : "Unknown";

            TempOpManager.OpData data = tempOpManager.getOpData(p.getUniqueId());
            String status = "[Permanent]"; // Default
            if (data != null) {
                if (data.type == TempOpManager.OpType.TEMP) {
                    status = "[Relog]";
                } else if (data.type == TempOpManager.OpType.TIME) {
                    long remaining = (data.expiration - System.currentTimeMillis()) / 1000;
                    if (remaining < 0)
                        remaining = 0;
                    status = "[" + formatDuration(remaining) + "]";
                }
            }

            String format = tempFormat.replace("%player%", name).replace("%status%", status);
            // If the user's config doesn't have %status%, they won't see it?
            // User asked "change the placeholder in /ops from [Permanent] to [Relog]".
            // This implies they currently use a placeholder or text that says [Permanent].
            // If they hardcoded [Permanent], I can't change it without them editing config.
            // UNLESS I replace "[Permanent]" text literal with status?
            // Let's try to replace "[Permanent]" too, just in case they hardcoded it.
            format = format.replace("[Permanent]", status);

            tempFormatted.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .serialize(plugin.parseText(format, p instanceof Player ? (Player) p : null)));
        }

        String permStr = String.join(", ", permFormatted);
        String tempStr = String.join(", ", tempFormatted);

        // Output from Config
        List<String> header = plugin.getConfigManager().getMessages()
                .getStringList("commands.op-manager.ops-list-format.header");
        List<String> middle = plugin.getConfigManager().getMessages()
                .getStringList("commands.op-manager.ops-list-format.middle");
        List<String> footer = plugin.getConfigManager().getMessages()
                .getStringList("commands.op-manager.ops-list-format.footer");

        for (String s : header)
            sender.sendMessage(plugin.parseText(s, (Player) sender));
        sender.sendMessage(plugin.parseText(permStr, (Player) sender));

        for (String s : middle)
            sender.sendMessage(plugin.parseText(s, (Player) sender));
        sender.sendMessage(plugin.parseText(tempStr, (Player) sender));

        for (String s : footer)
            sender.sendMessage(plugin.parseText(s, (Player) sender));
    }

    private String formatDuration(long seconds) {
        if (seconds >= 86400)
            return (seconds / 86400) + "d";
        if (seconds >= 3600)
            return (seconds / 3600) + "h";
        if (seconds >= 60)
            return (seconds / 60) + "m";
        return seconds + "s";
    }
}

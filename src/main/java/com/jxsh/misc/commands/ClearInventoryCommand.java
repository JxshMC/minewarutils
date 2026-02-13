package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearInventoryCommand extends BaseCommand {

    public ClearInventoryCommand(JxshMisc plugin) {
        super(plugin, "clearinventory", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "clearinventory.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        target.getInventory().clear();
        target.getInventory().setArmorContents(null);
        // Also clear offhand
        target.getInventory().setItemInOffHand(null);

        if (target.equals(sender)) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.clear-inventory.self"), target));
        } else {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("commands.clear-inventory.target")
                    .replace("%target%", target.getName());
            sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));

            String targetMsg = plugin.getConfigManager().getMessages().getString("commands.clear-inventory.self");
            target.sendMessage(plugin.parseText(targetMsg, target));
        }
    }
}

package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EatCommand extends BaseCommand {

    public EatCommand(JxshMisc plugin) {
        super(plugin, "eat", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        // Resolve target: arg 0 if present, else sender
        Player target = resolveTarget(sender, args, 0);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "eat.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);

        // Message logic
        if (target.equals(sender)) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.eat.self"), target));
        } else {
            String msg = plugin.getConfigManager().getMessages().getString("commands.eat.target");
            sender.sendMessage(plugin.parseText(msg.replace("%target%", target.getName()),
                    sender instanceof Player ? (Player) sender : null));
            target.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.eat.self"), target));
        }
    }
}

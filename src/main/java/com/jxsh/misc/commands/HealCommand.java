package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand extends BaseCommand {

    public HealCommand(JxshMisc plugin) {
        super(plugin, "heal", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "heal.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);

        if (target.equals(sender)) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.heal.self"), target));
        } else {
            String msg = plugin.getConfigManager().getMessages().getString("commands.heal.target");
            sender.sendMessage(plugin.parseText(msg.replace("%target%", target.getName()),
                    sender instanceof Player ? (Player) sender : null));
            target.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.heal.self"), target));
        }
    }
}

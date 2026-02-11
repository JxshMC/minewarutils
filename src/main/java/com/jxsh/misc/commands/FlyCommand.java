package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand extends BaseCommand {

    public FlyCommand(JxshMisc plugin) {
        super(plugin, "fly", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "fly.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        boolean newMode = !target.getAllowFlight();
        target.setAllowFlight(newMode);
        if (newMode) {
            target.setFlying(true);
        } else {
            target.setFlying(false);
        }

        if (target.equals(sender)) {
            String msg = newMode
                    ? plugin.getConfigManager().getMessages().getString("commands.fly.enabled")
                    : plugin.getConfigManager().getMessages().getString("commands.fly.disabled");
            sender.sendMessage(plugin.parseText(msg, target));
        } else {
            String msgKey = newMode ? "commands.fly.target-enabled" : "commands.fly.target-disabled";
            String msg = plugin.getConfigManager().getMessages().getString(msgKey).replace("%target%",
                    target.getName());

            sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));

            String targetMsg = newMode
                    ? plugin.getConfigManager().getMessages().getString("commands.fly.enabled")
                    : plugin.getConfigManager().getMessages().getString("commands.fly.disabled");
            target.sendMessage(plugin.parseText(targetMsg, target));
        }
    }
}

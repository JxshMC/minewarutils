package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class FlySpeedCommand extends BaseCommand {

    public FlySpeedCommand(JxshMisc plugin) {
        super(plugin, "flyspeed", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.flyspeed.usage"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        float speed;
        try {
            float input = Float.parseFloat(args[0]);
            if (input < 0 || input > 10) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.flyspeed.range"),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }
            // Minecraft scale is -1 to 1. Default walk is 0.2. Default fly is 0.1.
            // Requirement: Scale 0-10 to Minecraft internal.
            // Providing 1 -> 0.1
            // Providing 10 -> 1.0 (Very fast)
            speed = input / 10.0f;
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages()
                            .getString("commands.error.invalid-number").replace("%input%", args[0]),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        Player target = resolveTarget(sender, args, 1);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "flyspeed.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        target.setFlySpeed(speed);

        String inputStr = args[0];

        if (target.equals(sender)) {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("commands.flyspeed.self")
                    .replace("%speed%", inputStr);
            sender.sendMessage(plugin.parseText(msg, target));
        } else {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("commands.flyspeed.target")
                    .replace("%speed%", inputStr)
                    .replace("%target%", target.getName());
            sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));

            String targetMsg = plugin.getConfigManager().getMessages()
                    .getString("commands.flyspeed.self")
                    .replace("%speed%", inputStr);
            target.sendMessage(plugin.parseText(targetMsg, target));
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        }
        return null;
    }
}

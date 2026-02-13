package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SlowChatCommand extends BaseCommand {

    public SlowChatCommand(JxshMisc plugin) {
        super(plugin, "slowchat", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.slowchat.usage"),
                            (sender instanceof Player ? (Player) sender : null)));
            return;
        }

        if (args[0].equalsIgnoreCase("off")) {
            plugin.getChatManager().setSlowChatSeconds(0);
            Bukkit.broadcast(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("chat-control.slow-mode-disabled"),
                    (sender instanceof Player ? (Player) sender : null)));
            return;
        }

        try {
            int seconds = Integer.parseInt(args[0]);
            plugin.getChatManager().setSlowChatSeconds(seconds);
            String msg = plugin.getConfigManager().getMessages().getString("chat-control.slow-mode-enabled");
            Bukkit.broadcast(plugin.parseText(msg.replace("%seconds%", String.valueOf(seconds)),
                    (sender instanceof Player ? (Player) sender : null)));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.invalid-number")
                            .replace("%input%", args[0]),
                    (sender instanceof Player ? (Player) sender : null)));
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("off");
            options.add("5");
            options.add("10");
            options.add("30");
            return options;
        }
        return null;
    }
}

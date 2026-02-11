package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand extends BaseCommand {

    public ClearChatCommand(JxshMisc plugin) {
        super(plugin, "clearchat", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        int lines = plugin.getConfigManager().getMessages().getInt("chat-control.clear-chat-lines", 100);
        Component blankLine = Component.text(" ");

        String bypassNode = plugin.getBypassPermission("clearchat");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(bypassNode))
                continue;
            for (int i = 0; i < lines; i++) {
                player.sendMessage(blankLine);
            }
        }

        String broadcastMsg = plugin.getConfigManager().getMessages().getString("chat-control.clear-chat");
        Bukkit.broadcast(plugin.parseText(broadcastMsg.replace("%player%", sender.getName()),
                (sender instanceof Player ? (Player) sender : null)));
    }
}

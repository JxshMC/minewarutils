package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteChatCommand extends BaseCommand {

    public MuteChatCommand(JxshMisc plugin) {
        super(plugin, "mutechat", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Logic extracted from JxshMisc.onCommand
        boolean newState = !plugin.getChatManager().isChatMuted();
        plugin.getChatManager().setChatMuted(newState);

        String msgKey = newState ? "chat-control.mute-enabled" : "chat-control.mute-disabled";

        Bukkit.broadcast(plugin.parseText(plugin.getConfigManager().getMessages().getString(msgKey),
                (sender instanceof Player ? (Player) sender : null)));
    }
}

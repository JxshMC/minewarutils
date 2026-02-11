package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MentionToggleCommand extends BaseCommand {

    public MentionToggleCommand(JxshMisc plugin) {
        super(plugin, "mentiontoggle", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        plugin.toggleMention(player.getUniqueId());
        boolean enabled = plugin.isMentionEnabled(player.getUniqueId());

        String msgKey = enabled ? "mentions.enabled" : "mentions.disabled";

        player.sendMessage(
                plugin.parseText(plugin.getConfigManager().getMessages().getString(msgKey), player));
    }
}

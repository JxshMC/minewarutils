package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.KitManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateKitCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final KitManager kitManager;

    public CreateKitCommand(JxshMisc plugin, KitManager kitManager) {
        super(plugin, "createkit", true);
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.kit.create-usage"), player));
            return;
        }

        String name = args[0];
        if (kitManager.kitExists(name)) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.kit.exists"), player));
            return;
        }

        long cooldown = 0;
        if (args.length > 1) {
            try {
                cooldown = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("commands.error.invalid-number").replace("%input%", args[1]), player));
                return;
            }
        }

        kitManager.createKit(name, player, cooldown);
        sender.sendMessage(plugin.parseText(
                plugin.getConfigManager().getMessages().getString("commands.kit.created").replace("%kit%", name),
                player));
    }
}

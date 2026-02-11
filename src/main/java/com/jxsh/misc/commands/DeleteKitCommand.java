package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.KitManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeleteKitCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final KitManager kitManager;

    public DeleteKitCommand(JxshMisc plugin, KitManager kitManager) {
        super(plugin, "deletekit", true); // Or false if console allowed? Using true for consistency with player inputs
                                          // usually
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.kit.delete-usage"),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        String name = args[0];
        if (!kitManager.kitExists(name)) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.kit.not-found").replace("%kit%", name),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        kitManager.deleteKit(name);
        sender.sendMessage(plugin.parseText(
                plugin.getConfigManager().getMessages().getString("commands.kit.deleted").replace("%kit%", name),
                sender instanceof Player ? (Player) sender : null));
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return new java.util.ArrayList<>(plugin.getKitManager().getKits());
        }
        return java.util.Collections.emptyList();
    }
}

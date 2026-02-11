package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.KitManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditKitCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final KitManager kitManager;

    public EditKitCommand(JxshMisc plugin, KitManager kitManager) {
        super(plugin, "editkit", true);
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.kit.edit-usage"), player));
            return;
        }

        String name = args[0];
        if (!kitManager.kitExists(name)) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.kit.not-found").replace("%kit%", name),
                    player));
            return;
        }

        // Preserve cooldown? Or overwrite? Usually overwrite inventory, keep fields.
        // kitManager.createKit overwrites everything.
        // We should get old cooldown first.
        long cooldown = kitManager.getCooldown(name);

        kitManager.createKit(name, player, cooldown);
        sender.sendMessage(plugin.parseText(
                plugin.getConfigManager().getMessages().getString("commands.kit.edited").replace("%kit%", name),
                player));
    }
}

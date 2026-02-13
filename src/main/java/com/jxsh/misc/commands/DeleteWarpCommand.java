package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.WarpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeleteWarpCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final WarpManager warpManager;

    public DeleteWarpCommand(JxshMisc plugin, WarpManager warpManager) {
        super(plugin, "deletewarp", true);
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Player player = (Player) sender; // BaseCommand enforces IsPlayer=true

        if (args.length < 1) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.warps.delete-usage"), player));
            return;
        }

        String name = args[0];
        if (warpManager.deleteWarp(name)) { // Returns true if found and deleted
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.warps.deleted").replace("%warp%", name),
                    player));
        } else {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.warps.not-found").replace("%warp%", name), player));
        }
    }
}

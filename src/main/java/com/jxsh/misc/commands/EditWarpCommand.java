package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.WarpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditWarpCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final WarpManager warpManager;

    public EditWarpCommand(JxshMisc plugin, WarpManager warpManager) {
        super(plugin, "editwarp", true);
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.warps.edit-usage"), player));
            return;
        }

        String name = args[0];
        if (warpManager.getWarp(name) == null) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.warps.not-found").replace("%warp%", name), player));
            return;
        }

        // Overwrite
        warpManager.setWarp(name, player.getLocation());
        sender.sendMessage(plugin.parseText(
                plugin.getConfigManager().getMessages().getString("commands.warps.edited").replace("%warp%", name),
                player));
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return new java.util.ArrayList<>(plugin.getWarpManager().getWarps());
        }
        return java.util.Collections.emptyList();
    }
}

package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.WarpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final WarpManager warpManager;

    public SetWarpCommand(JxshMisc plugin, WarpManager warpManager) {
        super(plugin, "setwarp", true);
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if (args.length < 1) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.warps.set-usage"), (Player) sender));
            return;
        }

        Player player = (Player) sender;
        String warpName = args[0];

        warpManager.setWarp(warpName, player.getLocation());

        String msg = plugin.getConfigManager().getMessages().getString("commands.warps.set")
                .replace("%warp%", warpName);
        player.sendMessage(plugin.parseText(msg, player));
    }
}

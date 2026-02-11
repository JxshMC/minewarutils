package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.WarpManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class WarpsCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final WarpManager warpManager;

    public WarpsCommand(JxshMisc plugin, WarpManager warpManager) {
        super(plugin, "warp", false); // Use "warp" key for permissions (minewar.warp)
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Set<String> warps = warpManager.getWarps();
        if (warps.isEmpty()) {
            sender.sendMessage(
                    plugin.parseText("<red>No warps set.", sender instanceof Player ? (Player) sender : null));
            return;
        }

        String warpList = String.join(", ", warps);
        String msg = plugin.getConfigManager().getMessages().getString("commands.warps.list").replace("%warps%",
                warpList);
        sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
    }
}

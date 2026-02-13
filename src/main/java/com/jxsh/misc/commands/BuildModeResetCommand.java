package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.BuildModeManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildModeResetCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final BuildModeManager buildModeManager;

    public BuildModeResetCommand(JxshMisc plugin, BuildModeManager buildModeManager) {
        super(plugin, "bmreset", false); // Can be run by console? Logic below supports it
        this.plugin = plugin;
        this.buildModeManager = buildModeManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        // Permission check handled by BaseCommand

        int count = 0;
        String targetName = "Global";

        if (args.length > 0) {
            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                String errorMsg = plugin.getConfigManager().getMessages().getString("invalid-player",
                        "<red>Invalid player.");
                errorMsg = errorMsg.replace("%target%", args[0]);
                sender.sendMessage(plugin.parseText(errorMsg, sender instanceof Player ? (Player) sender : null));
                return;
            }
            count = buildModeManager.resetPlayer(target);
            targetName = target.getName();
        } else {
            count = buildModeManager.resetAllTrackedBlocks();
        }

        String msg = plugin.getConfigManager().getMessages().getString("buildmode.reset",
                "<green>Reset %count% blocks.");
        msg = msg.replace("%count%", String.valueOf(count)).replace("%target%", targetName);

        sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
    }
}

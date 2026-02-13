package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;

public class HelpCommand extends BaseCommand {

    public HelpCommand(JxshMisc plugin) {
        super(plugin, "help", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().getConfig().getBoolean("help-system.enabled", true)) {
            // Respect legacy setting if present
            return;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        plugin.getHelpManager().showHelp(sender, page);
    }
}

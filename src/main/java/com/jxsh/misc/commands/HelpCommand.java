package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

import java.util.Arrays;

public class HelpCommand extends BukkitCommand {

    private final JxshMisc plugin;

    public HelpCommand(JxshMisc plugin) {
        super("help");
        this.plugin = plugin;
        this.setDescription("Shows the help menu");
        this.setUsage("/help [page]");
        this.setAliases(Arrays.asList("?"));
        this.setPermission("minewar.help");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (plugin.getConfigManager() == null || plugin.getConfigManager().getConfig() == null) {
            sender.sendMessage("§cPlugin is still loading. Please wait...");
            return true;
        }

        if (!plugin.getConfigManager().getConfig().getBoolean("help-system.enabled", true)) {
            // If disabled via config but command is still registered (shouldn't happen if
            // logic is correct),
            // or if we want soft disable.
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        plugin.getHelpManager().showHelp(sender, page);
        return true;
    }
}

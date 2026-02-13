package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.ForcefieldManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForcefieldCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final ForcefieldManager forcefieldManager;

    public ForcefieldCommand(JxshMisc plugin, ForcefieldManager forcefieldManager) {
        super(plugin, "forcefield", true);
        this.plugin = plugin;
        this.forcefieldManager = forcefieldManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if (args.length > 0) {
            // Target specific player
            if (!plugin.hasPermission(sender, "forcefield.others")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }

            Player target = org.bukkit.Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.invalid-player")
                                .replace("%target%", args[0]),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }

            forcefieldManager.toggle(target);
            boolean enabled = forcefieldManager.isEnabled(target);

            String msgKey = enabled ? "commands.forcefield.enabled-other" : "commands.forcefield.disabled-other";
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString(msgKey).replace("%target%", target.getName()),
                    sender instanceof Player ? (Player) sender : null));

            // Optionally notify target? Maybe not needed for admin tool.

        } else {
            // Self toggle
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.no-console"), null));
                return;
            }
            Player player = (Player) sender;
            forcefieldManager.toggle(player);

            boolean enabled = forcefieldManager.isEnabled(player);
            if (enabled) {
                player.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.forcefield.enabled"), player));
            } else {
                player.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.forcefield.disabled"), player));
            }
        }
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && plugin.hasPermission(sender, "forcefield.others")) {
            return getOnlinePlayerNames(sender);
        }
        return java.util.Collections.emptyList();
    }
}

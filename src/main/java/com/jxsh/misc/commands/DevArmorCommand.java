package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.DevManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DevArmorCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final DevManager devManager;

    public DevArmorCommand(JxshMisc plugin, DevManager devManager) {
        super(plugin, "devarmour", true);
        this.plugin = plugin;
        this.devManager = devManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.players-only",
                            "<red>Only players."),
                    null));
            return;
        }

        if (args.length == 0) {
            toggle(player, player);
        } else {
            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                String msg = plugin.getConfigManager().getMessages().getString("commands.error.invalid-player");
                player.sendMessage(plugin.parseText(msg.replace("%target%", args[0]), player));
                return;
            }

            if (!target.equals(sender) && !plugin.hasPermission(sender, "devarmour.others")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }
            toggle(player, target);
        }
    }

    private void toggle(Player sender, Player target) {
        java.util.UUID uuid = target.getUniqueId();
        if (devManager.hasArmour(uuid)) {
            devManager.setArmour(uuid, false);
            target.getInventory().setHelmet(null);
            target.getInventory().setChestplate(null);
            target.getInventory().setLeggings(null);
            target.getInventory().setBoots(null);

            if (sender.equals(target)) {
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("dev.armour-off"), target));
            } else {
                String msg = plugin.getConfigManager().getMessages().getString("dev.armour-off-other");
                sender.sendMessage(plugin.parseText(msg.replace("%target%", target.getName()), target));
            }
        } else {
            devManager.setArmour(uuid, true);
            if (sender.equals(target)) {
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("dev.armour-on"), target));
            } else {
                String msg = plugin.getConfigManager().getMessages().getString("dev.armour-on-other");
                sender.sendMessage(plugin.parseText(msg.replace("%target%", target.getName()), target));
            }
        }
    }
}

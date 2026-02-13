package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.KitManager;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final KitManager kitManager;

    public KitCommand(JxshMisc plugin, KitManager kitManager) {
        super(plugin, "kit", true);
        this.plugin = plugin;
        this.kitManager = kitManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Player player = (Player) sender;
        Player target = player;

        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            listKits(player);
            return;
        }

        String kitName = args[0];

        if (args.length > 1) {
            if (!plugin.hasPermission(player, "kit.others")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                        player));
                return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("commands.error.invalid-player").replace("%target%", args[1]), player));
                return;
            }
        }

        if (!kitManager.kitExists(kitName)) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.kit.not-found").replace("%kit%", kitName), player));
            return;
        }

        // Per-kit permission check
        // If config says per-kit-permission is enabled
        // This check is slightly complex as BaseCommand handles main permission.
        // But for per-kit, we check here.
        if (plugin.getConfigManager().getConfig().getBoolean("Kits.per-kit-permission", true)) {
            if (!player.hasPermission("minewar.kit." + kitName.toLowerCase())) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.no-permission"), player));
                return;
            }
        }

        // Timer Check only if taking for self
        if (target == player) { // Admins giving kits might bypass cooldown logic generally, or we check
                                // cooldown for target? usually self.
            // Usually /kit name checks cooldown.
            if (kitManager.isOnCooldown(player, kitName)) {
                long remaining = kitManager.getRemainingTime(player, kitName);
                // Format functionality in messages.yml?
                String msg = plugin.getConfigManager().getMessages().getString("commands.kit.cooldown")
                        .replace("%time%", String.valueOf(remaining));
                player.sendMessage(plugin.parseText(msg, player));
                return;
            }
        }

        kitManager.giveKit(kitName, target);
        String msg = plugin.getConfigManager().getMessages().getString("commands.kit.received").replace("%kit%",
                kitName);
        target.sendMessage(plugin.parseText(msg, target));

        if (target != player) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.kit.given")
                    .replace("%kit%", kitName).replace("%target%", target.getName()), player));
        }
    }

    private void listKits(Player player) {
        Set<String> kits = plugin.getKitManager().getKits();

        if (kits.isEmpty()) {
            player.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.kit.none"),
                    player));
            return;
        }

        String listFormat = plugin.getConfigManager().getMessages().getString("commands.kit.list");

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String kit : kits) {
            sb.append("<click:run_command:/kit ").append(kit).append(">")
                    .append("<hover:show_text:'<#ccffff>Click to get <#0adef7>").append(kit).append("'>")
                    .append("<#0adef7>").append(kit)
                    .append("</hover></click>");

            if (i < kits.size() - 1) {
                sb.append("<gray>, ");
            }
            i++;
        }

        String msg = listFormat.replace("%kits%", sb.toString());
        player.sendMessage(plugin.parseText(msg, player));
    }
}

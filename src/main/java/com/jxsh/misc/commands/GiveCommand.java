package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GiveCommand extends BaseCommand {

    public GiveCommand(JxshMisc plugin) {
        super(plugin, "give", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        // Usage: /give <item> [amount] [player] [name...]
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.poopgun.players-only"), null));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.give.usage"),
                            sender instanceof Player ? (Player) sender : null));
            return;
        }

        // 1. Material
        String itemInput = args[0].toUpperCase();
        Material material = Material.matchMaterial(itemInput);
        if (material == null) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.give.invalid-item")
                            .replace("%item%", itemInput),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        // 2. Amount
        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1)
                    amount = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(
                        plugin.parseText(
                                plugin.getConfigManager().getMessages().getString("commands.error.invalid-number")
                                        .replace("%input%", args[1]),
                                sender instanceof Player ? (Player) sender : null));
                return;
            }
        }

        // 3. Target
        Player target = resolveTarget(sender, args, 2);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "give.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        // 4. Custom Name
        String customName = null;
        if (args.length > 3) {
            // Join args starting from index 3
            customName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        }

        ItemStack item = new ItemStack(material, amount);
        if (customName != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                Component nameComp = plugin.parseText(customName, target); // Use target for placeholders? or sender?
                                                                           // Using target seems standard for context.
                meta.displayName(nameComp);
                item.setItemMeta(meta);
            }
        }

        target.getInventory().addItem(item);

        String msg = plugin.getConfigManager().getMessages().getString("commands.give.success")
                .replace("%amount%", String.valueOf(amount))
                .replace("%material%", material.name())
                .replace("%player%", target.getName());

        sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Material suggestion
            return java.util.Arrays.stream(Material.values())
                    .map(Material::name)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (args.length == 3) {
            // Arg 2 (index 2) is player
            return getOnlinePlayerNames(sender);
        }
        return null;
    }
}

package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LoreCommand extends BaseCommand {

    private final JxshMisc plugin;

    public LoreCommand(JxshMisc plugin) {
        super(plugin, "lore", true);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.itemname.no-item"), player));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.lore.usage"), player));
            return;
        }

        String subCommand = args[0].toLowerCase();
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<Component> lore = meta.lore();
        if (lore == null)
            lore = new ArrayList<>();

        if (subCommand.equals("add")) {
            if (args.length < 2) {
                sender.sendMessage(plugin
                        .parseText(plugin.getConfigManager().getMessages().getString("commands.lore.usage"), player));
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String lineText = sb.toString().trim();

            lore.add(plugin.parseText(lineText, player));
            meta.lore(lore);
            item.setItemMeta(meta);
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.lore.added"), player));

        } else if (subCommand.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage(plugin
                        .parseText(plugin.getConfigManager().getMessages().getString("commands.lore.usage"), player));
                return;
            }

            int lineNum;
            try {
                lineNum = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.lore.invalid-line"), player));
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String lineText = sb.toString().trim();

            if (lineNum < 1) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.lore.invalid-line"), player));
                return;
            }

            // Auto-fill logic
            if (lineNum > lore.size()) {
                while (lore.size() < lineNum - 1) {
                    lore.add(Component.empty());
                }
                lore.add(plugin.parseText(lineText, player));
            } else {
                lore.set(lineNum - 1, plugin.parseText(lineText, player));
            }

            meta.lore(lore);
            item.setItemMeta(meta);

            String msg = plugin.getConfigManager().getMessages().getString("commands.lore.set").replace("%line%",
                    String.valueOf(lineNum));
            sender.sendMessage(plugin.parseText(msg, player));

        } else if (subCommand.equals("clear")) {
            meta.lore(null);
            item.setItemMeta(meta);
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.lore.cleared"), player));
        } else {
            sender.sendMessage(
                    plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.lore.usage"), player));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            sub.add("add");
            sub.add("set");
            sub.add("clear");
            return sub;
        }
        return new ArrayList<>();
    }
}

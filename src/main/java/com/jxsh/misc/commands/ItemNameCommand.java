package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameCommand extends BaseCommand {

    private final JxshMisc plugin;

    public ItemNameCommand(JxshMisc plugin) {
        super(plugin, "itemname", true);
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
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.itemname.usage"), player));
            return;
        }

        String name = String.join(" ", args);
        Component nameComponent = plugin.parseText(name, player);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(nameComponent);
            item.setItemMeta(meta);
            player.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.itemname.success"), player));
        }
    }
}

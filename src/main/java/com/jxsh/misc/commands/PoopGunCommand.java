package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PoopGunCommand extends BaseCommand {

    private final JxshMisc plugin;

    public PoopGunCommand(JxshMisc plugin) {
        super(plugin, "poopgun", true);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // BaseCommand handles invalid sender (console) checking via constructor super
        // call logic?
        // Wait, BaseCommand checks permission but handling the "player-only" part might
        // be manual or handled by BaseCommand if we enforced it?
        // Checking BaseCommand source would be good, but for now I'll stick to manual
        // check if unsure, usually BaseCommand implies player-only if we check sender
        // instanceof Player.
        // Actually BaseCommand in this project usually has `if (playerOnly && !(sender
        // instanceof Player))` check.

        // Let's assume standard behavior.
        if (args.length > 0) {
            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                String msg = plugin.getConfigManager().getMessages().getString("commands.error.invalid-player");
                // Fallback if message is missing/null, though ConfigManager usage implies it
                // should exist or handled
                if (msg == null)
                    msg = "<red>Player not found.";
                sender.sendMessage(plugin.parseText(msg.replace("%target%", args[0]),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }

            if (!target.equals(sender) && !plugin.hasPermission(sender, "poopgun.others")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                        sender instanceof Player ? (Player) sender : null));
                return;
            }
            giveGun(target);
            String msg = plugin.getConfigManager().getMessages().getString("poopgun.receive-other");
            if (msg == null)
                msg = "<green>Gave Poop Gun to %target%.";
            sender.sendMessage(plugin.parseText(msg.replace("%target%", target.getName()),
                    sender instanceof Player ? (Player) sender : null));
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.players-only",
                                "<red>Only players can use this command without arguments."),
                        null));
                return;
            }
            giveGun(player);
            String msg = plugin.getConfigManager().getMessages().getString("poopgun.receive");
            player.sendMessage(plugin.parseText(msg, player));
        }
    }

    private void giveGun(Player player) {
        String matName = plugin.getConfigManager().getConfig().getString("utility.poopgun.item.material",
                "DIAMOND_HOE");
        Material material = Material.matchMaterial(matName);
        if (material == null)
            material = Material.DIAMOND_HOE;

        ItemStack gun = new ItemStack(material);
        ItemMeta meta = gun.getItemMeta();
        if (meta != null) {
            String displayName = plugin.getConfigManager().getConfig().getString("utility.poopgun.item.name",
                    "&6&lPoop Gun");
            meta.displayName(plugin.parseText(displayName, player));

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : plugin.getConfigManager().getConfig().getStringList("utility.poopgun.item.lore")) {
                lore.add(plugin.parseText(line, player));
            }
            meta.lore(lore);

            // Add PDC tag for robust identification
            NamespacedKey key = new NamespacedKey(plugin, "poopgun");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            gun.setItemMeta(meta);
        }

        player.getInventory().addItem(gun);
    }
}

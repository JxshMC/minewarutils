package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class InventorySeeCommand extends BaseCommand {

    public InventorySeeCommand(JxshMisc plugin) {
        super(plugin, "inventorysee", true); // Player only
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        if (args.length == 0) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.inventorysee.usage"), (Player) sender));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(
                    plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("commands.error.invalid-player")
                                    .replace("%target%", args[0]),
                            (Player) sender));
            return;
        }

        boolean showArmor = false;
        if (args.length > 1 && args[1].equals("2")) {
            showArmor = true;
        }

        Player viewer = (Player) sender;

        if (showArmor) {
            openArmorGUI(viewer, target);
        } else {
            viewer.openInventory(target.getInventory());
        }
    }

    private void openArmorGUI(Player viewer, Player target) {
        Inventory armorInv = Bukkit.createInventory(null, 9,
                plugin.parseText("<dark_gray>Armor: " + target.getName(), null));
        ItemStack[] armor = target.getInventory().getArmorContents();

        if (armor.length >= 4) {
            armorInv.setItem(0, armor[3]); // Helmet
            armorInv.setItem(1, armor[2]); // Chest
            armorInv.setItem(2, armor[1]); // Leg
            armorInv.setItem(3, armor[0]); // Boot
        }

        armorInv.setItem(8, target.getInventory().getItemInOffHand());

        viewer.openInventory(armorInv);
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("1", "2");
        }
        return null;
    }
}

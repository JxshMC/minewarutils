package com.jxsh.misc.tasks;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.DevManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.UUID;

public class ArmourTask extends BukkitRunnable {

    private final JxshMisc plugin;
    private final DevManager devManager;
    private final Random random = new Random();

    public ArmourTask(JxshMisc plugin, DevManager devManager) {
        this.plugin = plugin;
        this.devManager = devManager;
    }

    @Override
    public void run() {
        java.util.Iterator<UUID> iterator = devManager.getActiveArmour().keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            player.getInventory().setHelmet(createArmour(Material.LEATHER_HELMET, getRandomBrightColor()));
            player.getInventory().setChestplate(createArmour(Material.LEATHER_CHESTPLATE, getRandomBrightColor()));
            player.getInventory().setLeggings(createArmour(Material.LEATHER_LEGGINGS, getRandomBrightColor()));
            player.getInventory().setBoots(createArmour(Material.LEATHER_BOOTS, getRandomBrightColor()));
        }
    }

    private Color getRandomBrightColor() {
        java.awt.Color hsbColor = java.awt.Color.getHSBColor(random.nextFloat(), 1.0f, 1.0f);
        return Color.fromRGB(hsbColor.getRed(), hsbColor.getGreen(), hsbColor.getBlue());
    }

    private ItemStack createArmour(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }
}

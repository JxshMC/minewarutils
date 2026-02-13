package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PoopGunListener implements Listener {

    private final JxshMisc plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> activePoopers = new HashSet<>();

    public PoopGunListener(JxshMisc plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // 1. Check for PDC Tag (Recommended)
        NamespacedKey key = new NamespacedKey(plugin, "poopgun");
        boolean isPoopGun = meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);

        // 2. Fallback to Display Name (for older items)
        if (!isPoopGun && meta.hasDisplayName()) {
            String configNameRaw = plugin.getConfigManager().getConfig().getString("fun.poop-gun.item.name",
                    "&6&lPoop Gun");

            // Convert both to plain text for comparison
            String plainConfigName = translateToPlainText(configNameRaw);
            String plainDisplayName = translateToPlainText(
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                            .serialize(meta.displayName()));

            if (plainDisplayName.contains(plainConfigName) || plainDisplayName.equalsIgnoreCase(plainConfigName)) {
                isPoopGun = true;
            }
        }

        if (!isPoopGun) {
            return;
        }

        event.setCancelled(true);

        // Cooldown Check
        if (cooldowns.containsKey(player.getUniqueId())) {
            long remaining = cooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
            if (remaining > 0) {
                // Optionally send action bar or message? Config doesn't specify, but good UX.
                // User said "cooldown will be enough" implying silent fail or generic feedback?
                // I'll leave it silent or standard "You can't do that yet" if requested later.
                return;
            }
        }

        int cooldownSeconds = plugin.getConfigManager().getConfig().getInt("fun.poop-gun.shoot-cooldown", 5);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000L));

        // Fire snowball
        Snowball snowball = player.launchProjectile(Snowball.class);

        // Velocity logic
        double velocity = plugin.getConfigManager().getConfig().getDouble("fun.poop-gun.velocity", 1.5);
        snowball.setVelocity(player.getLocation().getDirection().multiply(velocity));

        snowball.setMetadata("poopgun_projectile", new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onSnowballHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball snowball))
            return;
        if (!(event.getEntity() instanceof Player target))
            return;

        if (!snowball.hasMetadata("poopgun_projectile"))
            return;

        event.setCancelled(true); // Don't hurt them, just make them poop

        if (activePoopers.contains(target.getUniqueId())) {
            // Already pooping, just ignore (cooldown on shooter prevents spam, but this
            // protects target from multiple shooters)
            return;
        }

        startPoopingSession(target);

        if (snowball.getShooter() instanceof Player shooter) {
            String msg = plugin.getConfigManager().getMessages().getString("poopgun.hit-target",
                    "<green>You made %target% poop!");
            shooter.sendMessage(plugin.parseText(msg.replace("%target%", target.getName()), shooter));
        }

        String hitMsg = plugin.getConfigManager().getMessages().getString("poopgun.hit-by",
                "<red>You were hit by a poop gun! Oh no!");
        target.sendMessage(plugin.parseText(hitMsg, target));

        // Global Message
        String globalMsg = plugin.getConfigManager().getMessages().getString("poopgun.global-msg");
        if (globalMsg != null && !globalMsg.isEmpty()) {
            plugin.getServer().broadcast(plugin.parseText(globalMsg.replace("%target%", target.getName()), target));
        }
    }

    private void startPoopingSession(Player target) {
        UUID uuid = target.getUniqueId();
        activePoopers.add(uuid);

        int duration = Integer
                .parseInt(plugin.getConfigManager().getConfig().getString("fun.poop-gun.item.poop-duration", "15"));
        int interval = 3; // Hardcoded or config? Previous code had it. Let's keep 3s interval for
                          // 'pulse'.
        // Actually user said "poop-duration is 15".

        int maxPoops = 50; // Safety limit
        String poopNameTemplate = plugin.getConfigManager().getMessages().getString("poopgun.poop-name",
                "&6%target%'s shit");

        String soundName = plugin.getConfigManager().getConfig().getString("fun.poop-gun.sound",
                "minecraft:entity.player.burp");
        float volume = Float
                .parseFloat(plugin.getConfigManager().getConfig().getString("fun.poop-gun.volume", "100"));
        float pitch = Float.parseFloat(plugin.getConfigManager().getConfig().getString("fun.poop-gun.pitch", "0.1"));

        new BukkitRunnable() {
            private int elapsed = 0;
            private final List<Item> itemsOnFloor = new LinkedList<>();

            @Override
            public void run() {
                if (!target.isOnline() || elapsed >= duration) {
                    cleanup();
                    this.cancel();
                    return;
                }

                // Spawn a poop
                ItemStack poopItem = new ItemStack(Material.COCOA_BEANS);
                ItemMeta meta = poopItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(plugin.parseText(poopNameTemplate.replace("%target%", target.getName()), target));
                    poopItem.setItemMeta(meta);
                }

                Item dropped = target.getWorld().dropItem(target.getLocation(), poopItem);
                dropped.setPickupDelay(Integer.MAX_VALUE); // Cannot pick up
                dropped.customName(plugin.parseText(poopNameTemplate.replace("%target%", target.getName()), target));
                dropped.setCustomNameVisible(true);
                itemsOnFloor.add(dropped);

                // Play sound
                try {
                    target.getWorld().playSound(target.getLocation(), soundName, volume, pitch);
                } catch (Exception e) {
                    // Invalid sound
                }

                // Enforce max poops
                while (itemsOnFloor.size() > maxPoops) {
                    Item oldest = itemsOnFloor.remove(0);
                    if (oldest.isValid())
                        oldest.remove();
                }

                elapsed += interval;
            }

            private void cleanup() {
                activePoopers.remove(uuid);
                for (Item item : itemsOnFloor) {
                    if (item.isValid())
                        item.remove();
                }
            }
        }.runTaskTimer(plugin, 0L, interval * 20L);
    }

    private String translateToPlainText(String text) {
        if (text == null)
            return "";
        // Remove MiniMessage tags and legacy codes
        return text.replaceAll("<[^>]*>", "").replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
    }
}

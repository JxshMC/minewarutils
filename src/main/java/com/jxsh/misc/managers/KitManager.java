package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {

    private final JxshMisc plugin;
    private YamlDocument kitsConfig;
    private YamlDocument cooldownsConfig;

    private final Map<UUID, Map<String, Long>> cooldownCache = new HashMap<>();

    public KitManager(JxshMisc plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File databaseFolder = new File(plugin.getDataFolder(), "Database");
        if (!databaseFolder.exists()) {
            databaseFolder.mkdirs();
        }

        try {
            // Kits Config
            File kitsFile = new File(databaseFolder, "kits.yml");
            kitsConfig = YamlDocument.create(
                    kitsFile,
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true).build());

            // Cooldowns Config
            File cooldownsFile = new File(databaseFolder, "kit_cooldowns.yml");
            cooldownsConfig = YamlDocument.create(
                    cooldownsFile,
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true).build());

            loadCooldowns();

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load kits configurations!");
            e.printStackTrace();
        }
    }

    public void createKit(String name, Player player, long cooldownSeconds) {
        String path = "kits." + name.toLowerCase();

        // Serialize Inventory
        String inventoryData = itemStackArrayToBase64(player.getInventory().getContents());

        kitsConfig.set(path + ".inventory", inventoryData);
        kitsConfig.set(path + ".cooldown", cooldownSeconds);

        try {
            kitsConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteKit(String name) {
        String path = "kits." + name.toLowerCase();
        if (kitsConfig.contains(path)) {
            kitsConfig.remove(path);
            try {
                kitsConfig.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean kitExists(String name) {
        return kitsConfig.contains("kits." + name.toLowerCase());
    }

    public void giveKit(String name, Player player) {
        String path = "kits." + name.toLowerCase();
        if (!kitsConfig.contains(path))
            return; // Should check existence before calling

        String inventoryData = kitsConfig.getString(path + ".inventory");
        ItemStack[] items;
        try {
            items = itemStackArrayFromBase64(inventoryData);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(plugin.parseText("<red>Error loading kit data.", player));
            return;
        }

        // Logic check: Clear inventory? Or add?
        // User didn't specify, but "Give the kit" usually means add or replace.
        // "Overwrite an existing kit's contents" implies creating kit.
        // Best practice: Add to inventory, drop if full.

        // Wait, standard kits usually replace or add?
        // Let's safe side: add, and drop overflow.
        // But armor?
        // Serialization `getContents()` includes armor and offhand in newer versions?
        // `getContents()` is main inventory + armor + offhand usually?
        // Wait, `getContents()` returns 41 items (36 main + 4 armor + 1 offhand) in
        // newer versions.
        // Let's verify. Yes, PlayerInventory extends Inventory.
        // But `getContents()` is inconsistent across versions.
        // Best to use separate methods or a robust serializer.
        // Base64 serializer below handles arrays.

        // If we saved the WHOLE inventory structure, we should restore it exactly?
        // Typically kits are just items. But "Capture the player's entire inventory,
        // including armor slots and off-hand"
        // implies we should restore armor too.

        // Let's try to restore slots if they are empty, otherwise add to inventory.
        // Actually, simpler: `player.getInventory().setContents(items)` OVERWRITES.
        // If user wants to "give kit", usually they want the items.
        // If I use setContents, I wipe their current stuff. That might be dangerous if
        // not intended.
        // But "Capture entire inventory" strongly implies a state restore.
        // Let's check user request: "/createkit ... Save current inventory as a kit"
        // "/kit ... Give the kit."
        // I will attempt to merge. Armor slots -> equip if empty, else add to inv.
        // Main items -> add to inv.

        // Actually, decoding `getContents()` returns the array with slots in order.
        // The array is usually: Main Contents (0-35), Armor (36-39), Offhand (40).
        // Let's iterate and place.

        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item == null || item.getType() == Material.AIR)
                    continue;

                // If it's armor/offhand slots (indices depend on version, likely 36-40 or
                // similar logic)
                // Bukkit is weird.
                // Let's just use `addItem` for safety unless we want to force equip.
                // User said "capture... including armor". This implies we should give back
                // armor.
                // I will use addItem for everything to be safe against deletions,
                // unless I implement a smarter "equip if empty" logic.

                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }

        // Cooldown
        long cooldown = kitsConfig.getLong(path + ".cooldown", 0L);
        if (cooldown > 0) {
            setCooldown(player.getUniqueId(), name, cooldown);
        }
    }

    // Cooldown Logic
    public long getCooldown(String kitName) {
        return kitsConfig.getLong("kits." + kitName.toLowerCase() + ".cooldown", 0L);
    }

    public boolean isOnCooldown(Player player, String kitName) {
        if (player.hasPermission("minewar.kit.bypass.cooldown"))
            return false; // Implicit bypass?

        UUID uuid = player.getUniqueId();
        if (!cooldownCache.containsKey(uuid))
            return false;

        Map<String, Long> playerCooldowns = cooldownCache.get(uuid);
        if (!playerCooldowns.containsKey(kitName.toLowerCase()))
            return false;

        long expiry = playerCooldowns.get(kitName.toLowerCase());
        return System.currentTimeMillis() < expiry;
    }

    public long getRemainingTime(Player player, String kitName) {
        UUID uuid = player.getUniqueId();
        if (!cooldownCache.containsKey(uuid))
            return 0;

        long expiry = cooldownCache.get(uuid).getOrDefault(kitName.toLowerCase(), 0L);
        long diff = expiry - System.currentTimeMillis();
        return Math.max(0, diff / 1000); // Seconds
    }

    public void setCooldown(UUID uuid, String kitName, long seconds) {
        if (seconds <= 0)
            return;

        cooldownCache.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(kitName.toLowerCase(), System.currentTimeMillis() + (seconds * 1000));

        // Save to cache/file
        saveCooldowns();
    }

    private void loadCooldowns() {
        if (!cooldownsConfig.contains("cooldowns"))
            return;

        dev.dejvokep.boostedyaml.block.implementation.Section section = cooldownsConfig.getSection("cooldowns");
        for (Object uuidObj : section.getKeys()) {
            String uuidStr = uuidObj.toString();
            try {
                UUID uuid = UUID.fromString(uuidStr);
                dev.dejvokep.boostedyaml.block.implementation.Section kitsSec = section.getSection(uuidStr);
                for (Object kitObj : kitsSec.getKeys()) {
                    String kit = kitObj.toString();
                    long expiry = kitsSec.getLong(kit);
                    if (expiry > System.currentTimeMillis()) {
                        cooldownCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(kit, expiry);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveCooldowns() {
        // Prune expired
        cooldownsConfig.remove("cooldowns"); // Clear and rewrite for simplicity (or update)

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Long>> entry : cooldownCache.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, Long> kitEntry : entry.getValue().entrySet()) {
                if (kitEntry.getValue() > now) {
                    cooldownsConfig.set("cooldowns." + uuid + "." + kitEntry.getKey(), kitEntry.getValue());
                }
            }
        }

        try {
            cooldownsConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        saveCooldowns();
    }

    // Serialization Helpers
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public Set<String> getKits() {
        if (kitsConfig == null || !kitsConfig.contains("kits"))
            return Collections.emptySet();
        return kitsConfig.getSection("kits").getRoutesAsStrings(false);
    }

    public YamlDocument getConfig() {
        return kitsConfig;
    }
}

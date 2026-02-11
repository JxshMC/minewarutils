package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TempOpManager {

    private final JxshMisc plugin;
    private YamlDocument tempOpsConfig;

    public enum OpType {
        PERM,
        TEMP, // Basic: Deop on giver/receiver quit
        TIME // Timer: Deop after time expiration
    }

    public static class OpData {
        public OpType type;
        public UUID giver;
        public long expiration; // 0 for PERM/TEMP

        public OpData(OpType type, UUID giver, long expiration) {
            this.type = type;
            this.giver = giver;
            this.expiration = expiration;
        }
    }

    // Map<ReceiverUUID, OpData>
    private final Map<UUID, OpData> activeOps = new ConcurrentHashMap<>();

    public Set<UUID> getTempOps() {
        return Collections.unmodifiableSet(activeOps.keySet());
    }

    public TempOpManager(JxshMisc plugin) {
        this.plugin = plugin;
        load();
        startCleanupTask();
    }

    public void load() {
        File databaseFolder = new File(plugin.getDataFolder(), "Database");
        if (!databaseFolder.exists()) {
            databaseFolder.mkdirs();
        }

        try {
            File file = new File(databaseFolder, "tempop.yml");
            tempOpsConfig = YamlDocument.create(
                    file,
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true).build());

            loadFromConfig();

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load tempop.yml!");
            e.printStackTrace();
        }
    }

    private void loadFromConfig() {
        activeOps.clear();
        if (!tempOpsConfig.contains("active-ops"))
            return;

        dev.dejvokep.boostedyaml.block.implementation.Section section = tempOpsConfig.getSection("active-ops");
        for (Object key : section.getKeys()) {
            String receiverStr = key.toString();
            try {
                UUID receiverUUID = UUID.fromString(receiverStr);
                String typeStr = section.getString(receiverStr + ".type");
                String giverStr = section.getString(receiverStr + ".giver");
                long expiration = section.getLong(receiverStr + ".expiration", 0L);

                OpType type = OpType.valueOf(typeStr);
                UUID giverUUID = giverStr != null ? UUID.fromString(giverStr) : null;

                activeOps.put(receiverUUID, new OpData(type, giverUUID, expiration));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load tempop data for " + receiverStr);
            }
        }
    }

    public void save() {
        tempOpsConfig.remove("active-ops");
        for (Map.Entry<UUID, OpData> entry : activeOps.entrySet()) {
            String path = "active-ops." + entry.getKey().toString();
            OpData data = entry.getValue();
            tempOpsConfig.set(path + ".type", data.type.name());
            if (data.giver != null) {
                tempOpsConfig.set(path + ".giver", data.giver.toString());
            }
            tempOpsConfig.set(path + ".expiration", data.expiration);
        }

        try {
            tempOpsConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void grantOp(Player giver, Player receiver, OpType type, long durationSeconds) {
        if (receiver.isOp()) {
            // Check override logic if needed
        }

        receiver.setOp(true);
        long expiration = (type == OpType.TIME) ? System.currentTimeMillis() + (durationSeconds * 1000L) : 0;

        activeOps.put(receiver.getUniqueId(), new OpData(type, giver.getUniqueId(), expiration));
        save();

        // Messages
        String giverName = giver.getName();
        String receiverName = receiver.getName();

        if (type == OpType.TEMP) {
            // Session Op Messages
            receiver.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.op-manager.grant")
                    .replace("%target%", giverName), receiver));

            giver.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.op-manager.grant-sender")
                    .replace("%target%", receiverName), giver));
        } else {
            // Time/Perm Op Messages
            String timeStr = (type == OpType.PERM) ? "Permanent" : formatDuration(durationSeconds);

            receiver.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.tempop.granted-target")
                    .replace("%time%", timeStr), receiver));

            giver.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.tempop.granted")
                    .replace("%target%", receiverName)
                    .replace("%time%", timeStr), giver));
        }
    }

    private String formatDuration(long seconds) {
        if (seconds >= 86400)
            return (seconds / 86400) + "d";
        if (seconds >= 3600)
            return (seconds / 3600) + "h";
        if (seconds >= 60)
            return (seconds / 60) + "m";
        return seconds + "s";
    }

    public void revokeOp(UUID receiverUUID) {
        // We need to know who revoked it, or why?
        // Logic might need to be shifted if we want "sponsor-left" message.
        // Overloading this or handling it in caller is better.
        revokeOp(receiverUUID, null);
    }

    public void revokeOp(UUID receiverUUID, String reasonMsgKey) {
        OpData data = activeOps.remove(receiverUUID); // Get data before removing
        save();

        Player receiver = Bukkit.getPlayer(receiverUUID);
        OfflinePlayer offlineReceiver = Bukkit.getOfflinePlayer(receiverUUID);

        if (receiver != null) {
            receiver.setOp(false);

            String msgKey = (reasonMsgKey != null) ? reasonMsgKey : "commands.op-manager.revoke";
            // Check if we need to replace %target% (the giver)
            String giverName = "Console";
            if (data != null && data.giver != null) {
                OfflinePlayer giver = Bukkit.getOfflinePlayer(data.giver);
                if (giver.getName() != null)
                    giverName = giver.getName();
            }

            receiver.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString(msgKey, "<red>Your OP has been revoked.")
                    .replace("%target%", giverName), receiver));
        } else {
            offlineReceiver.setOp(false);
        }
    }

    public OpData getOpData(UUID receiverUUID) {
        return activeOps.get(receiverUUID);
    }

    public boolean isTempOpp(UUID uuid) {
        return activeOps.containsKey(uuid);
    }

    // Logic for Quit Events
    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        // 1. If this player is a RECEIVER of a TEMP op -> Deop
        if (activeOps.containsKey(uuid)) {
            OpData data = activeOps.get(uuid);
            if (data.type == OpType.TEMP) {
                revokeOp(uuid);
            }
            // TIME ops are NOT revoked on quit.
        }

        // 2. If this player is a GIVER of a TEMP op -> Deop the receiver
        // We need to iterate
        List<UUID> toRevoke = new ArrayList<>();
        for (Map.Entry<UUID, OpData> entry : activeOps.entrySet()) {
            if (entry.getValue().type == OpType.TEMP && uuid.equals(entry.getValue().giver)) {
                toRevoke.add(entry.getKey());
            }
        }
        for (UUID rev : toRevoke) {
            revokeOp(rev, "commands.op-manager.sponsor-left");
        }
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                List<UUID> toRevoke = new ArrayList<>();

                for (Map.Entry<UUID, OpData> entry : activeOps.entrySet()) {
                    if (entry.getValue().type == OpType.TIME) {
                        if (entry.getValue().expiration > 0 && now > entry.getValue().expiration) {
                            toRevoke.add(entry.getKey());
                        }
                    }
                }

                for (UUID uuid : toRevoke) {
                    revokeOp(uuid);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Every second
    }

    public void shutdown() {
        save();
    }
}

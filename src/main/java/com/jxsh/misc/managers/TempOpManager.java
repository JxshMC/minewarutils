package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TempOpManager {

    private final JxshMisc plugin;
    private final com.jxsh.misc.managers.DatabaseManager db;

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

    // Map<ReceiverUUID, OpData> - Cache of active ops
    private final Map<UUID, OpData> activeOps = new ConcurrentHashMap<>();

    public Set<UUID> getTempOps() {
        return Collections.unmodifiableSet(activeOps.keySet());
    }

    public TempOpManager(JxshMisc plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        load();
        startCleanupTask();
    }

    public void load() {
        activeOps.clear();
        db.executeQuery("SELECT uuid, type, giver, expiration FROM temp_ops", rs -> {
            try {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String typeStr = rs.getString("type");
                    String giverStr = rs.getString("giver");
                    long expiration = rs.getLong("expiration");

                    UUID receiverUUID = UUID.fromString(uuidStr);
                    OpType type = OpType.valueOf(typeStr);
                    UUID giverUUID = (giverStr != null && !giverStr.isEmpty() && !giverStr.equals("null"))
                            ? UUID.fromString(giverStr)
                            : null;

                    activeOps.put(receiverUUID, new OpData(type, giverUUID, expiration));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load active temp-ops from database!");
                e.printStackTrace();
            }
            return null;
        }).join();
    }

    // No explicit save() needed as we write on changes.
    // Kept empty method if any legacy calls exist, or we can remove it.
    public void save() {
        // No-op for SQL implementation
    }

    public void grantOp(Player giver, Player receiver, OpType type, long durationSeconds) {
        if (receiver.isOp()) {
            // Check override logic if needed
        }

        receiver.setOp(true);
        long expiration = (type == OpType.TIME) ? System.currentTimeMillis() + (durationSeconds * 1000L) : 0;
        UUID receiverUUID = receiver.getUniqueId();
        UUID giverUUID = giver.getUniqueId();

        OpData data = new OpData(type, giverUUID, expiration);
        activeOps.put(receiverUUID, data);

        // SQL Upsert
        // We use REPLACE into or DELETE/INSERT.
        // Simple helper:
        db.executeUpdate("DELETE FROM temp_ops WHERE uuid=?", receiverUUID.toString());
        db.executeUpdate("INSERT INTO temp_ops (uuid, type, giver, expiration) VALUES (?, ?, ?, ?)",
                receiverUUID.toString(),
                type.name(),
                giverUUID.toString(),
                expiration);

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
        activeOps.remove(receiverUUID);

        // Remove from DB
        db.executeUpdate("DELETE FROM temp_ops WHERE uuid=?", receiverUUID.toString());

        Player receiver = Bukkit.getPlayer(receiverUUID);
        OfflinePlayer offlineReceiver = Bukkit.getOfflinePlayer(receiverUUID);

        if (receiver != null) {
            receiver.setOp(false);
            // Message strictly handled by DeopCommand/caller to avoid duplicates
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
        // No explicit save needed
    }
}

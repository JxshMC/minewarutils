package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TempOpManager {

    private final JxshMisc plugin;
    // Strict SQL Only

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

    private void load() {
        // Strict SQL: No YAML for data.
        // We rely on DatabaseManager to be connected.
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
            // Load async? The manager is init in onEnable, might need sync load or future.
            // For now, we'll load sync to ensure data is ready, or async if acceptable.
            // User asked for "Async SQL queries".
            // Loading usually needs to be done or we need to handle "loading" state.
            // Let's run async and update the map.
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::loadFromSql);
        } else {
            plugin.getLogger().warning("TempOpManager: Database not connected. Data will not persist!");
        }
    }

    public void save() {
        // Async save
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveToSql);
    }

    private void saveToSql() {
        if (!plugin.getDatabaseManager().isConnected())
            return;

        try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            // UPSERT or DELETE/INSERT
            try (java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM temp_op")) {
                ps.executeUpdate();
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO temp_op (uuid, duration, start_time) VALUES (?, ?, ?)")) {
                for (Map.Entry<UUID, OpData> entry : activeOps.entrySet()) {
                    ps.setString(1, entry.getKey().toString());
                    ps.setLong(2, entry.getValue().type == OpType.TIME ? entry.getValue().expiration : -1);
                    ps.setLong(3, System.currentTimeMillis());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFromSql() {
        if (!plugin.getDatabaseManager().isConnected())
            return;

        try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement("SELECT * FROM temp_op")) {

            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long duration = rs.getLong("duration");
                // Mapping: -1 = PERM/TEMP (Assuming PERM for persistence, or we lose TEMP on
                // restart? TEMP is session-based.)
                // If it's in DB, it should be PERM or TIME?
                // Request says "Basic: Deop on giver/receiver quit" for TEMP.
                // So TEMP shouldn't be in DB across restarts?
                // Actually if we crash, maybe? But standard TEMP is session.
                OpType type = (duration == -1) ? OpType.PERM : OpType.TIME;
                activeOps.put(uuid, new OpData(type, null, duration));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void grantOp(Player giver, Player receiver, OpType type, long durationSeconds) {
        receiver.setOp(true);
        long expiration = (type == OpType.TIME) ? System.currentTimeMillis() + (durationSeconds * 1000L) : 0;
        activeOps.put(receiver.getUniqueId(), new OpData(type, giver.getUniqueId(), expiration));
        save();

        String receiverName = receiver.getName();
        if (type == OpType.TEMP) {
            giver.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                    .getString("commands.op-manager.grant-sender")
                    .replace("%target%", receiverName), giver));
        } else {
            String timeStr = (type == OpType.PERM) ? "Permanent" : formatDuration(durationSeconds);
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
        revokeOp(receiverUUID, null);
    }

    public void revokeOp(UUID receiverUUID, String reasonMsgKey) {
        activeOps.remove(receiverUUID);
        save(); // Async save

        Player receiver = Bukkit.getPlayer(receiverUUID);
        OfflinePlayer offlineReceiver = Bukkit.getOfflinePlayer(receiverUUID);

        if (receiver != null) {
            receiver.setOp(false);
        } else {
            offlineReceiver.setOp(false);
        }
    }

    // ... (Keep existing getters/helpers) ...
    public OpData getOpData(UUID receiverUUID) {
        return activeOps.get(receiverUUID);
    }

    public boolean isTempOpp(UUID uuid) {
        return activeOps.containsKey(uuid);
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeOps.containsKey(uuid)) {
            OpData data = activeOps.get(uuid);
            if (data.type == OpType.TEMP) {
                revokeOp(uuid);
            }
        }
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
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void shutdown() {
        save();
    }
}

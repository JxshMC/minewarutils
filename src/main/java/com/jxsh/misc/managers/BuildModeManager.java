package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuildModeManager {

    private final JxshMisc plugin;

    // Sets of UUIDs for player states
    private final Set<UUID> buildModePlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> adminModePlayers = Collections.synchronizedSet(new HashSet<>());

    // Map Player UUID -> List of Changes
    private final Map<UUID, List<BlockChangeRecord>> playerChanges = new ConcurrentHashMap<>();

    // Global lookup for simple "is tracked" checks (Legacy support & fast lookup)
    private final Map<String, UUID> trackedLocations = new ConcurrentHashMap<>();

    private boolean dirty = false;
    private BukkitTask saveTask;

    public static class BlockChangeRecord {
        public String world;
        public int x, y, z;
        public String previousBlockData;

        public BlockChangeRecord(Location loc, BlockData oldData) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.previousBlockData = oldData.getAsString();
        }
    }

    public BuildModeManager(JxshMisc plugin) {
        this.plugin = plugin;

        loadDatabase();

        // Auto-save every 30 seconds
        this.saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (dirty) {
                    saveDatabase();
                    dirty = false;
                }
            }
        }.runTaskTimerAsynchronously(plugin, 600L, 600L);
    }

    public void shutdown() {
        if (saveTask != null && !saveTask.isCancelled()) {
            saveTask.cancel();
        }
        // Force save on disable
        saveDatabase();
    }

    public boolean isBuildModeEnabled(UUID uuid) {
        return buildModePlayers.contains(uuid);
    }

    public boolean isAdminModeEnabled(UUID uuid) {
        return adminModePlayers.contains(uuid);
    }

    public void setBuildModeEnabled(Player player, boolean enabled) {
        if (enabled) {
            buildModePlayers.add(player.getUniqueId());
            player.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.buildmode.enabled"), player));
        } else {
            buildModePlayers.remove(player.getUniqueId());
            player.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.buildmode.disabled"), player));
        }
        dirty = true;
    }

    public void setAdminMode(Player player, boolean enabled) {
        if (enabled) {
            adminModePlayers.add(player.getUniqueId());
            player.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.adminbuild.enabled"), player));
        } else {
            adminModePlayers.remove(player.getUniqueId());
            player.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.adminbuild.disabled"), player));
        }
    }

    public void recordChange(Player player, Block block, BlockData dataToRecord) {
        if (!isBuildModeEnabled(player.getUniqueId()))
            return;

        UUID uuid = player.getUniqueId();
        playerChanges.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));

        List<BlockChangeRecord> changes = playerChanges.get(uuid);
        synchronized (changes) {
            changes.add(new BlockChangeRecord(block.getLocation(), dataToRecord));
        }
        trackedLocations.put(serializeLocation(block.getLocation()), uuid);
        dirty = true;
    }

    public boolean isTrackedBlock(Block block) {
        return trackedLocations.containsKey(serializeLocation(block.getLocation()));
    }

    public void removeTrackedBlock(Block block) {
        trackedLocations.remove(serializeLocation(block.getLocation()));
    }

    public int getChangeCount(UUID uuid) {
        List<BlockChangeRecord> list = playerChanges.get(uuid);
        return list == null ? 0 : list.size();
    }

    public int resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        List<BlockChangeRecord> changes = playerChanges.remove(uuid);

        if (changes != null && !changes.isEmpty()) {
            int count = changes.size();
            // Undo changes logic
            plugin.getLogger().info("[BuildMode] Restoring " + count + " blocks for " + player.getName());

            // Reverse order to undo correctly
            Collections.reverse(changes);
            for (BlockChangeRecord record : changes) {
                World w = Bukkit.getWorld(record.world);
                if (w != null) {
                    Block b = w.getBlockAt(record.x, record.y, record.z);
                    try {
                        b.setBlockData(Bukkit.createBlockData(record.previousBlockData));
                    } catch (Exception e) {
                        // Ignore invalid data
                    }
                    trackedLocations.remove(serializeLocation(b.getLocation()));
                }
            }
            buildModePlayers.remove(uuid);
            executeExternalResetCommands(player);
            dirty = true;
            return count;
        }

        buildModePlayers.remove(uuid);
        executeExternalResetCommands(player);
        dirty = true;
        return 0;
    }

    public int resetAll() {
        int total = 0;
        for (UUID uuid : new HashSet<>(playerChanges.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                total += resetPlayer(p);
            }
        }
        return total;
    }

    private synchronized void saveDatabase() {
        saveToSql();
    }

    private void saveToSql() {
        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().isConnected())
            return;

        try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);

            String type = plugin.getConfigManager().getConfig().getString("storage.type", "H2").toUpperCase();
            boolean isMaria = type.equals("MARIADB");

            try (java.sql.PreparedStatement ps = conn.prepareStatement(isMaria
                    ? "INSERT INTO buildmode_data (uuid, blocks_placed, active) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE blocks_placed=?, active=?"
                    : "MERGE INTO buildmode_data (uuid, blocks_placed, active) KEY(uuid) VALUES (?, ?, ?)")) {

                synchronized (buildModePlayers) {
                    for (UUID uuid : buildModePlayers) {
                        ps.setString(1, uuid.toString());
                        ps.setInt(2, getChangeCount(uuid));
                        ps.setBoolean(3, true);
                        if (isMaria) {
                            ps.setInt(4, getChangeCount(uuid));
                            ps.setBoolean(5, true);
                        }
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDatabase() {
        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().isConnected()) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::loadDatabase, 100L);
            return;
        }
        loadFromSql();
    }

    private void loadFromSql() {
        try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                java.sql.PreparedStatement ps = conn
                        .prepareStatement("SELECT uuid FROM buildmode_data WHERE active = true")) {

            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                buildModePlayers.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return "";
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private void executeExternalResetCommands(Player player) {
        List<String> commands = plugin.getConfigManager().getConfig().getStringList("buildmode.reset-commands");
        if (commands == null || commands.isEmpty())
            return;

        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String cmd : commands) {
            String finalCmd = cmd.replace("%player%", player.getName());
            try {
                Bukkit.dispatchCommand(console, finalCmd);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute reset command: " + finalCmd);
                e.printStackTrace();
            }
        }
    }
}

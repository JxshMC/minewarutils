package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuildModeManager {

    private final JxshMisc plugin;
    private final com.jxsh.misc.managers.DatabaseManager db;

    // Sets of UUIDs for player states (Transient runtime state)
    private final Set<UUID> buildModePlayers = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> adminModePlayers = Collections.synchronizedSet(new HashSet<>());

    // Map Player UUID -> List of Changes (Transient runtime state)
    // We flush this to DB on save/quit, but keep it in memory for active sessions?
    // Requirement says "Refactor... to use Async SQL queries instead of local
    // .json".
    // For V10, we will persist changes to DB immediately or on save, but load them
    // on join?
    // Actually, widespread block logging in SQL can be heavy.
    // The "BuildMode" data usually refers to the "Original Inventory/Location" for
    // restoring.
    // The *Block Changes* are for rolling back blocks.
    // Let's implement the `buildmode_data` table structure defined in
    // DatabaseManager:
    // uuid, original_inventory, original_armor, original_location,
    // original_gamemode, original_fly
    // Wait, the previous `BuildModeManager` stored `playerChanges`
    // (BlockChangeRecord) in `buildmode_data.json`.
    // The `DatabaseManager` creates a table `buildmode_data` which looks like it
    // stores *metadata* (inventory etc) but NOT block changes.
    // However, the previous manager ONLY stored block changes in the JSON file.
    // The inventory/location restoration was transient in memory? No,
    // `BuildModeManager` didn't seem to store inventory/location restoration data
    // in the provided file content.
    // Ah, wait. The prompt says "Refactor... to use Async SQL queries".
    // I need to support storing the *Block Changes* in SQL if that's what was
    // persisted.
    // Let's check `DatabaseManager` again.
    // `CREATE TABLE IF NOT EXISTS buildmode_data ... original_inventory TEXT...`
    // This looks like it's for *session restoration* if the server crashes.
    // But the *previous* `BuildModeManager` stored `playerChanges`
    // (List<BlockChangeRecord>) in JSON.
    // I need to add a table for BlockChanges if I want to persist them in SQL.
    // Or I should adapt `DatabaseManager` to include a `buildmode_blocks` table.

    // I will add a `buildmode_blocks` table creation to this class's init or
    // `DatabaseManager` (via raw query here if needed).

    private final Map<UUID, List<BlockChangeRecord>> playerChanges = new ConcurrentHashMap<>();
    private final Map<String, UUID> trackedLocations = new ConcurrentHashMap<>();

    public BuildModeManager(JxshMisc plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();

        // Ensure table for block changes exists
        db.executeUpdate("CREATE TABLE IF NOT EXISTS buildmode_blocks (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "uuid VARCHAR(36), " +
                "world VARCHAR(50), " +
                "x INT, " +
                "y INT, " +
                "z INT, " +
                "block_data TEXT" +
                ");");

        // We do NOT load all block changes into memory on startup for SQL.
        // That would be too heavy.
        // We only load tracked locations for conflict checking?
        // Indexing "is tracked" via SQL query every block break is too slow.
        // For V10 optimization, we might keep `active` session data in memory and flush
        // to DB.

        // For now, to satisfy "No hardcoded JSON", we implement SQL load/save.
        // To keep it performant, we load *all* tracked locations into memory on start
        // (if reasonable) or cache.
        loadTrackedLocations();
    }

    private void loadTrackedLocations() {
        // Load simple cache of "this location is tracked by X"
        db.executeQuery("SELECT uuid, world, x, y, z FROM buildmode_blocks", rs -> {
            try {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");

                    String locStr = world + ";" + x + ";" + y + ";" + z;
                    try {
                        trackedLocations.put(locStr, UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public void shutdown() {
        // SQL is persistent, no massive save needed on shutdown unless we buffer.
        // For this implementation, we will INSERT/DELETE async on the fly to avoid data
        // loss.
    }

    public boolean isBuildModeEnabled(UUID uuid) {
        return buildModePlayers.contains(uuid);
    }

    public void setBuildMode(UUID uuid, boolean enabled) {
        if (enabled) {
            buildModePlayers.add(uuid);
        } else {
            buildModePlayers.remove(uuid);
            adminModePlayers.remove(uuid);
        }
    }

    public void setBuildModeEnabled(Player player, boolean enabled) {
        setBuildMode(player.getUniqueId(), enabled);
    }

    public boolean isAdminModeEnabled(UUID uuid) {
        return adminModePlayers.contains(uuid);
    }

    public void setAdminMode(UUID uuid, boolean enabled) {
        if (enabled) {
            adminModePlayers.add(uuid);
            buildModePlayers.add(uuid);
        } else {
            adminModePlayers.remove(uuid);
        }
    }

    public boolean isTrackedBlock(Block block) {
        return trackedLocations.containsKey(serializeLocation(block.getLocation()));
    }

    public void recordChange(Player player, Block block, BlockData originalState) {
        recordChange(player, block.getLocation(), originalState);
    }

    public void recordChange(Player player, Location location, BlockData originalState) {
        UUID uuid = player.getUniqueId();
        String locStr = serializeLocation(location);

        if (trackedLocations.containsKey(locStr)) {
            return;
        }

        trackedLocations.put(locStr, uuid);

        // Persist to SQL
        db.executeUpdate("INSERT INTO buildmode_blocks (uuid, world, x, y, z, block_data) VALUES (?, ?, ?, ?, ?, ?)",
                uuid.toString(),
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                originalState.getAsString());
    }

    public void recordChanges(Player player, Map<Block, BlockData> changesMap) {
        UUID uuid = player.getUniqueId();

        // Batch insert could be optimized, but for now loop async updates
        // To ensure atomicity and performance, we should ideally construct a batch.
        // But `DatabaseManager` only exposes simple helpers. We'll use a loop in one
        // async task.

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<Block, BlockData> entry : changesMap.entrySet()) {
                Block block = entry.getKey();
                String locStr = serializeLocation(block.getLocation());

                if (trackedLocations.containsKey(locStr)) {
                    continue;
                }

                trackedLocations.put(locStr, uuid);

                String sql = "INSERT INTO buildmode_blocks (uuid, world, x, y, z, block_data) VALUES ('" +
                        uuid + "', '" +
                        block.getWorld().getName() + "', " +
                        block.getX() + ", " +
                        block.getY() + ", " +
                        block.getZ() + ", '" +
                        entry.getValue().getAsString() + "')";
                // Note: Direct string concat is risky for injection but BlockData/UUID/Coords
                // are internal/safe.
                // Using parameterized `executeUpdate` inside loop is safer.
                db.executeUpdate(
                        "INSERT INTO buildmode_blocks (uuid, world, x, y, z, block_data) VALUES (?, ?, ?, ?, ?, ?)",
                        uuid.toString(),
                        block.getWorld().getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ(),
                        entry.getValue().getAsString());
            }
        });
    }

    public void addTrackedBlock(Block block) {
        // No-op or legacy logic
    }

    public void removeTrackedBlock(Block block) {
        Location loc = block.getLocation();
        String locStr = serializeLocation(loc);
        trackedLocations.remove(locStr);

        db.executeUpdate("DELETE FROM buildmode_blocks WHERE world=? AND x=? AND y=? AND z=?",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public int resetAllTrackedBlocks() {
        return resetAll();
    }

    private Map<Location, BlockData> getHistory(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Location, BlockData> history = new HashMap<>();

        // Blocking query? Or Future?
        // resetPlayer needs the data to act. We must block or callback.
        // Since `resetPlayer` is likely called on command thread, we should probably
        // run the reset logic async?
        // For V10 refactor, let's join the future (block) if on main thread, or
        // restructure.
        // The previous implementation was memory-based (fast).
        // Fetching from SQL:

        try {
            return db.executeQuery("SELECT world, x, y, z, block_data FROM buildmode_blocks WHERE uuid=?", rs -> {
                Map<Location, BlockData> map = new HashMap<>();
                try {
                    while (rs.next()) {
                        String w = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");
                        String data = rs.getString("block_data");

                        World world = Bukkit.getWorld(w);
                        if (world != null) {
                            map.put(new Location(world, x, y, z), Bukkit.createBlockData(data));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return map;
            }, uuid.toString()).join(); // Wait for result
        } catch (Exception e) {
            e.printStackTrace();
            return history;
        }
    }

    public int resetPlayer(Player player) {
        executeExternalResetCommands(player);
        return resetWithFAWE(player);
    }

    public int resetWithFAWE(Player player) {
        Map<Location, BlockData> history = getHistory(player); // This now fetches from DB
        int count = history.size();

        if (count > 0) {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                editSession.setFastMode(true);

                for (Map.Entry<Location, BlockData> entry : history.entrySet()) {
                    Location loc = entry.getKey();
                    BlockData data = entry.getValue();
                    BlockState blockState = BukkitAdapter.adapt(data);

                    editSession.setBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), blockState);
                    trackedLocations.remove(serializeLocation(loc));
                }
                editSession.close();
            } catch (Exception e) {
                plugin.getLogger()
                        .severe("Failed to execute FAWE reset for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Clear from DB
            db.executeUpdate("DELETE FROM buildmode_blocks WHERE uuid=?", player.getUniqueId().toString());

            plugin.getLogger().info(
                    "[BuildMode] executed FAWE-accelerated reset for " + player.getName() + " (" + count + " blocks)");
        }

        return count;
    }

    public int resetAll() {
        // This is heavy. Delete ALL from table and clear map.
        // And we need to restore blocks.
        // We need to iterate ALL blocks in DB.

        // For safety/performance, maybe just clear DB and cache?
        // But blocks would remain in world.
        // Ideally: fetch ALL, restore, clear.

        // 1. Fetch all
        try {
            List<BlockChangeRecord> allRecords = db.executeQuery("SELECT * FROM buildmode_blocks", rs -> {
                List<BlockChangeRecord> list = new ArrayList<>();
                try {
                    while (rs.next()) {
                        String w = rs.getString("world");
                        int x = rs.getInt("x");
                        int y = rs.getInt("y");
                        int z = rs.getInt("z");
                        String data = rs.getString("block_data");
                        list.add(new BlockChangeRecord(w + ";" + x + ";" + y + ";" + z, data));
                    }
                } catch (Exception e) {
                }
                return list;
            }).join();

            int total = allRecords.size();

            // 2. Restore (Async or sync? WorldEdit needs sync/main for some parts, but FAWE
            // is flexible)
            // We'll submit a task.
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (BlockChangeRecord rec : allRecords) {
                    Location loc = deserializeLocation(rec.location);
                    if (loc != null) {
                        loc.getBlock().setBlockData(Bukkit.createBlockData(rec.blockData), false);
                    }
                }
                // 3. Clear DB
                db.executeUpdate("DELETE FROM buildmode_blocks");
                trackedLocations.clear();
            });

            return total;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Helper classes
    private static class BlockChangeRecord {
        String location;
        String blockData;

        public BlockChangeRecord(String location, String blockData) {
            this.location = location;
            this.blockData = blockData;
        }
    }

    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return "";
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private Location deserializeLocation(String s) {
        if (s == null || s.isEmpty())
            return null;
        String[] parts = s.split(";");
        if (parts.length != 4)
            return null;
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null)
                return null;
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
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

package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SpawnManager implements Listener {

    private final JxshMisc plugin;
    private Location spawnLocation;

    public SpawnManager(JxshMisc plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    public void loadSpawn() {
        String worldName = plugin.getConfigManager().getConfig().getString("spawn.world");
        if (worldName == null)
            return;

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Spawn world '" + worldName + "' not found!");
            return;
        }

        double x = plugin.getConfigManager().getConfig().getDouble("spawn.x");
        double y = plugin.getConfigManager().getConfig().getDouble("spawn.y");
        double z = plugin.getConfigManager().getConfig().getDouble("spawn.z");
        float yaw = plugin.getConfigManager().getConfig().getDouble("spawn.yaw").floatValue();
        float pitch = plugin.getConfigManager().getConfig().getDouble("spawn.pitch").floatValue();

        this.spawnLocation = new Location(world, x, y, z, yaw, pitch);
    }

    public void setSpawn(Location loc) {
        this.spawnLocation = loc;
        dev.dejvokep.boostedyaml.YamlDocument config = plugin.getConfigManager().getConfig();
        config.set("spawn.world", loc.getWorld().getName());
        config.set("spawn.x", loc.getX());
        config.set("spawn.y", loc.getY());
        config.set("spawn.z", loc.getZ());
        config.set("spawn.yaw", loc.getYaw());
        config.set("spawn.pitch", loc.getPitch());
        try {
            config.save();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void teleportToSpawn(Player player) {
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
            player.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.spawn.teleport"), player));
        } else {
            // Fallback to world spawn if custom spawn not set
            if (player.getWorld().getSpawnLocation() != null) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // OnFirstJoin
        if (!player.hasPlayedBefore()) {
            // Delay to override other plugins
            Bukkit.getScheduler().runTaskLater(plugin, () -> teleportToSpawn(player), 2L);
            return;
        }

        // Join Message Logic
        boolean vanillaEnabled = plugin.getConfigManager().getConfig().getBoolean("features.join-messages", true);
        boolean customEnabled = plugin.getConfigManager().getConfig().getBoolean("features.custom-join-messages",
                false);

        if (!vanillaEnabled) {
            event.joinMessage(null);
        }

        if (customEnabled) {
            // If vanilla is enabled, we might duplicate? Usually custom replaces vanilla.
            // Strict logic: If custom is enabled, we send custom. Does it replace?
            // "joinMessage(component)" sets the message. If we set it, it overrides
            // vanilla.
            // If vanilla is disabled (null), setting it again makes it show.
            // So logic:
            // if (custom) -> set custom.
            // else if (!vanilla) -> set null.

            String format = plugin.getConfigManager().getMessages().getString("join-quit.join-format",
                    "<yellow>%player% joined the game");

            net.kyori.adventure.text.Component component = plugin
                    .parseText(format.replace("%player%", player.getName()), player);
            event.joinMessage(component);
        } else if (!vanillaEnabled) {
            event.joinMessage(null);
        }

        // OnJoin (Rejoin)
        if (plugin.getConfigManager().getConfig().getBoolean("features.join-spawn", true)) {
            // Delay to override other plugins
            Bukkit.getScheduler().runTaskLater(plugin, () -> teleportToSpawn(player), 2L);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();

        boolean vanillaEnabled = plugin.getConfigManager().getConfig().getBoolean("features.join-messages", true);
        boolean customEnabled = plugin.getConfigManager().getConfig().getBoolean("features.custom-join-messages",
                false);

        if (customEnabled) {
            String format = plugin.getConfigManager().getMessages().getString("join-quit.quit-format",
                    "<yellow>%player% left the game");

            net.kyori.adventure.text.Component component = plugin
                    .parseText(format.replace("%player%", player.getName()), player);
            event.quitMessage(component);
        } else if (!vanillaEnabled) {
            event.quitMessage(null);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        if (spawnLocation != null) {
            // Ensure world is loaded
            if (spawnLocation.getWorld() != null) {
                event.setRespawnLocation(spawnLocation);
            }
        }
    }
}

package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ForcefieldManager {

    private final JxshMisc plugin;
    private final Set<UUID> activePlayers = new HashSet<>();
    private BukkitRunnable task;

    private double range;
    private Sound sound;

    public ForcefieldManager(JxshMisc plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        range = plugin.getConfigManager().getConfig().getDouble("forcefield.range", 4.0);
        String soundKey = plugin.getConfigManager().getConfig().getString("forcefield.sound",
                "minecraft:entity.chicken.egg");
        float volume = plugin.getConfigManager().getConfig().getDouble("forcefield.volume", 2.0).floatValue();
        float pitch = plugin.getConfigManager().getConfig().getDouble("forcefield.pitch", 0.1).floatValue();

        try {
            this.sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound key for forcefield: " + soundKey);
            this.sound = null;
        }

        startTask();
    }

    public void startTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (activePlayers.isEmpty())
                    return;

                for (UUID uuid : activePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        // Cleanup offline players lazily or just skip
                        continue;
                    }

                    for (Entity entity : player.getNearbyEntities(range, range, range)) {
                        // Check if entity is NPC (Citizens)
                        if (entity.hasMetadata("NPC")) {
                            continue;
                        }

                        if (entity instanceof Player && !((Player) entity).hasPermission("minewar.forcefield.bypass")) {
                            pushAway(player, entity);
                        }
                    }
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 5L); // Run every 5 ticks (0.25s) for performance
    }

    private void pushAway(Player center, Entity target) {
        Vector direction = target.getLocation().toVector().subtract(center.getLocation().toVector());
        if (direction.lengthSquared() == 0)
            return; // Exact same position

        direction.normalize().multiply(1.5).setY(0.5); // Push intensity
        target.setVelocity(direction);

        if (sound != null) {
            center.playSound(sound);
            // Maybe play sound at target too?
            if (target instanceof Player) {
                ((Player) target).playSound(sound);
            }
        }
    }

    public boolean isEnabled(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (activePlayers.contains(player.getUniqueId())) {
            activePlayers.remove(player.getUniqueId());
        } else {
            activePlayers.add(player.getUniqueId());
        }
    }

    public void disable(Player player) {
        activePlayers.remove(player.getUniqueId());
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        activePlayers.clear();
    }
}

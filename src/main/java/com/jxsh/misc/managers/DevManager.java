package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DevManager {

    private final JxshMisc plugin;
    private final ConcurrentHashMap<UUID, Boolean> activeArmour = new ConcurrentHashMap<>();
    private final HashSet<UUID> devs;

    public DevManager(JxshMisc plugin) {
        this.plugin = plugin;
        this.devs = new HashSet<>();
        startShuffleTask();
    }

    public ConcurrentHashMap<UUID, Boolean> getActiveArmour() {
        return activeArmour;
    }

    public boolean hasArmour(UUID uuid) {
        return activeArmour.getOrDefault(uuid, false);
    }

    public void setArmour(UUID uuid, boolean active) {
        if (active) {
            activeArmour.put(uuid, true);
        } else {
            activeArmour.remove(uuid);
        }
    }

    private void startShuffleTask() {
        int speed = plugin.getConfigManager().getConfig().getInt("fun.dev-armor.shuffle-speed", 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                // If speed changed, could reload, but for now fixed on start
                for (UUID uuid : devs) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) {
                        // Assuming shuffleArmor method exists elsewhere or will be added
                        // shuffleArmor(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, speed);
    }
}```

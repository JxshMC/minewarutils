package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DevManager {

    private final JxshMisc plugin;
    private final ConcurrentHashMap<UUID, Boolean> activeArmour = new ConcurrentHashMap<>();

    public DevManager(JxshMisc plugin) {
        this.plugin = plugin;
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
}

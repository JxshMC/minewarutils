package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.TempOpManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TempOpListener implements Listener {

    private final JxshMisc plugin;
    private final TempOpManager tempOpManager;

    public TempOpListener(JxshMisc plugin, TempOpManager tempOpManager) {
        this.plugin = plugin;
        this.tempOpManager = tempOpManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tempOpManager.onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();

        if (tempOpManager.isTempOpp(uuid)) {
            com.jxsh.misc.managers.TempOpManager.OpData data = tempOpManager.getOpData(uuid);

            // If they are supposed to have TIME op, ensure they have it
            if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TIME) {
                if (System.currentTimeMillis() < data.expiration) {
                    if (!event.getPlayer().isOp()) {
                        event.getPlayer().setOp(true);
                        // Maybe send a reminder?
                        // event.getPlayer().sendMessage(plugin.parseText("<green>Welcome back! Your
                        // TempOp is still active.", event.getPlayer()));
                    }
                } else {
                    // Expired while offline
                    tempOpManager.revokeOp(uuid);
                }
            }
            // If they have TEMP (session based), they should have been revoked on quit.
            // But if server crashed or something, revoke now.
            else if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TEMP) {
                tempOpManager.revokeOp(uuid);
            }
        }
    }
}

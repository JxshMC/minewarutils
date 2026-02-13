package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.List;

public class MOTDListener implements Listener {

    private final JxshMisc plugin;

    public MOTDListener(JxshMisc plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("MOTD")) {
            return;
        }

        List<String> lines = plugin.getConfigManager().getMessages().getStringList("MOTD");
        if (lines == null || lines.isEmpty()) {
            return;
        }

        // Combine lines with newline
        // Note: ServerListPingEvent in Paper uses Component, but setMotd expects String
        // or Component depending on version?
        // In modern Paper, it's setMotd(Component). In Bukkit/Spigot it might be
        // String.
        // Let's assume Paper/Adventure support from JxshMisc structure.

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }

        Component motd = plugin.parseText(sb.toString(), null);
        event.setMotd(LegacyComponentSerializer.legacySection().serialize(motd));
        // Wait, setMotd takes String (legacy) usually in Bukkit event.
        // Paper has event.motd(Component).
        // Let's check if we can use the adventure method if available, or fallback to
        // legacy string.
        // But for safety and since JxshMisc uses "parseText" which returns Component...
        // Typical ServerListPingEvent only has setMotd(String).
        // PaperServerListPingEvent has component support.
        // Let's stick to standard event and serialize to Legacy Section (incase of
        // colors).
    }
}

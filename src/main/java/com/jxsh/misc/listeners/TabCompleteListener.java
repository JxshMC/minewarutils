package com.jxsh.misc.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.jxsh.misc.JxshMisc;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabCompleteListener implements Listener {

    private final JxshMisc plugin;

    public TabCompleteListener(JxshMisc plugin) {
        this.plugin = plugin;
    }

    /**
     * Blocks tab-completion suggestions for commands.
     * This prevents plugin leaking and unauthorized command discovery.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        // 1. Allow console and players with bypass permission
        if (event.getSender().hasPermission("minewar.admin.tabcomplete")) {
            return;
        }

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/"))
            return;

        // Parse command name (e.g. "/spawn arg" -> "spawn")
        String label = buffer.split(" ")[0].substring(1).toLowerCase();

        // Check if allowed
        List<String> allowedCommands = plugin.getConfigManager().getConfig()
                .getStringList("tab-complete.allowed-commands");
        if (allowedCommands == null)
            allowedCommands = Collections.emptyList();

        boolean allowed = allowedCommands.contains(label);

        // Also check permissions
        if (!allowed) {
            org.bukkit.command.PluginCommand pluginCmd = plugin.getServer().getPluginCommand(label);
            if (pluginCmd != null && pluginCmd.testPermissionSilent(event.getSender())) {
                allowed = true;
            }
        }

        if (!allowed) {
            event.setCompletions(Collections.emptyList());
            event.setHandled(true);
        }
    }

    /**
     * Controls what commands the client thinks are valid.
     * This affects the syntax highlighting (white/red) in the chat bar.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!event.getPlayer().hasPermission("minewar.admin.tabcomplete")) {
            // Get list of allowed commands from config
            List<String> allowedCommands = plugin.getConfigManager().getConfig()
                    .getStringList("tab-complete.allowed-commands");
            if (allowedCommands == null)
                allowedCommands = Collections.emptyList();

            List<String> toRemove = new ArrayList<>();
            for (String cmd : event.getCommands()) {
                // 1. If in allowed list, keep it.
                if (allowedCommands.contains(cmd)) {
                    continue;
                }

                // 2. Check permission for the command
                org.bukkit.command.PluginCommand pluginCmd = plugin.getServer().getPluginCommand(cmd);
                if (pluginCmd != null) {
                    if (pluginCmd.testPermissionSilent(event.getPlayer())) {
                        // User has permission, keep it.
                        continue;
                    }
                }

                // 3. Fallback: If not allowed explicitly and no permission granted -> Remove
                // Note: This hides vanilla commands not in allowed list (good for security)
                toRemove.add(cmd);
            }
            event.getCommands().removeAll(toRemove);
        }
    }
}

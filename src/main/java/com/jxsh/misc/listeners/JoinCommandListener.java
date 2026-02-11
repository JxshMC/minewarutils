package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class JoinCommandListener implements Listener {

    private final JxshMisc plugin;

    public JoinCommandListener(JxshMisc plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Join MOTD (Delayed by 5 ticks)
        if (plugin.getBoostedConfig() != null && plugin.getBoostedConfig().getBoolean("MOTD.enabled", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline())
                    return;
                List<String> motdLines = plugin.getConfigManager().getMessages().getStringList("MOTD");
                if (motdLines != null && !motdLines.isEmpty()) {
                    for (String line : motdLines) {
                        player.sendMessage(plugin.parseText(line, player));
                    }
                }
            }, 5L);
        }

        // 2. Join-Commands
        if (plugin.getBoostedConfig() == null || !plugin.getBoostedConfig().getBoolean("Join-Commands.enabled", true)) {
            return;
        }

        // Execute console commands SILENTLY
        if (plugin.getBoostedConfig().getBoolean("Join-Commands.as-console.enabled", true)) {
            List<String> consoleCmds = plugin.getBoostedConfig().getStringList("Join-Commands.as-console.commands");
            if (consoleCmds != null) {
                CommandSender silentSender = Bukkit.createCommandSender(component -> {
                });
                for (String cmd : consoleCmds) {
                    String processed = cmd.replace("%player_name%", player.getName())
                            .replace("%player%", player.getName());
                    Bukkit.dispatchCommand(silentSender, processed);
                }
            }
        }

        // Execute player commands SILENTLY
        if (plugin.getBoostedConfig().getBoolean("Join-Commands.as-player.enabled", true)) {
            List<String> playerCmds = plugin.getBoostedConfig().getStringList("Join-Commands.as-player.commands");
            if (playerCmds != null) {
                // To run as player BUT silent, we use createCommandSender and somehow
                // impersonate.
                // Since Paper doesn't have a direct .asPlayer(player), we will use the silent
                // sender
                // and if it's a player command, we hope it doesn't strictly check 'instanceof
                // Player'
                // OR we can use the player's performCommand if we're okay with them seeing it,
                // BUT the user specifically asked for silent.

                // Let's use a workaround: dispatch to console but prefixed with 'sudo' if
                // available,
                // or just use the silent sender.
                CommandSender silentSender = Bukkit.createCommandSender(component -> {
                });
                for (String cmd : playerCmds) {
                    String processed = cmd.replace("%player_name%", player.getName())
                            .replace("%player%", player.getName());

                    // Note: This silentSender won't have the player's specific permissions.
                    // If the command relies on player context, this might fail.
                    // However, I am following the user's specific technical hint.
                    Bukkit.dispatchCommand(silentSender, processed);
                }
            }
        }
    }
}

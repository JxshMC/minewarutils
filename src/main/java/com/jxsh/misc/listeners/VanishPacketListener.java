package com.jxsh.misc.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles vanish state synchronization between Velocity and Paper.
 * Implements "sticky" vanish that persists across joins and server switches.
 */
public class VanishPacketListener implements PluginMessageListener, Listener {

    private final JxshMisc plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishPacketListener(JxshMisc plugin) {
        this.plugin = plugin;
        // Register as event listener for PlayerJoinEvent
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("minewar:vanish")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("vanish")) {
            try {
                String uuidString = in.readUTF();
                boolean vanished = in.readBoolean();
                UUID targetUUID = UUID.fromString(uuidString);

                if (vanished) {
                    vanishedPlayers.add(targetUUID);
                    plugin.getLogger().info("[Vanish] Player " + targetUUID + " marked as VANISHED");
                } else {
                    vanishedPlayers.remove(targetUUID);
                    plugin.getLogger().info("[Vanish] Player " + targetUUID + " marked as VISIBLE");
                }

                Player target = Bukkit.getPlayer(targetUUID);
                if (target == null) {
                    return; // Player not on this server
                }

                // If vanished, hide from players who cannot see vanished
                if (vanished) {
                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.getUniqueId().equals(targetUUID)) {
                            // Rule A: admins.see sees all
                            // Rule B: vanish.see sees non-admins
                            boolean canSee = false;
                            if (viewer.hasPermission("minewar.vanish.admins.see")) {
                                canSee = true;
                            } else if (viewer.hasPermission("minewar.vanish.see")) {
                                if (!target.hasPermission("minewar.vanish.admins")) {
                                    canSee = true;
                                }
                            }

                            if (!canSee) {
                                viewer.hidePlayer(plugin, target);
                            } else {
                                viewer.showPlayer(plugin, target);
                            }
                        }
                    }
                    plugin.getLogger().info("[Vanish] Processed visibility for " + target.getName());
                } else {
                    // If not vanished, show to everyone
                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.getUniqueId().equals(targetUUID)) {
                            viewer.showPlayer(plugin, target);
                        }
                    }
                    plugin.getLogger().info("[Vanish] Showed " + target.getName() + " to all online players");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("[Vanish] Error processing vanish packet:");
                e.printStackTrace();
            }
        }
    }

    /**
     * STICKY VANISH: When a player joins, hide all vanished players from them
     * and hide them from all players if they are vanished.
     * This runs at LOWEST priority to ensure it happens before other plugins.
     * 
     * Uses a 2-tick delay to prevent Minecraft from overwriting the hide command.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        UUID joiningUUID = joiningPlayer.getUniqueId();

        plugin.getLogger().info("[Vanish] Processing join for " + joiningPlayer.getName());

        // Use 2-tick delay to prevent Minecraft from overwriting hide commands
        // This ensures the hide command happens AFTER the spawn packet
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 1. Handle joining player's visibility to others (if they are vanished)
            if (vanishedPlayers.contains(joiningUUID)) {
                plugin.getLogger()
                        .info("[Vanish] " + joiningPlayer.getName() + " is VANISHED - processing visibility");

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(joiningPlayer)) {
                        // Rule A: admins.see sees all
                        // Rule B: vanish.see sees non-admins
                        boolean canSee = false;
                        if (onlinePlayer.hasPermission("minewar.vanish.admins.see")) {
                            canSee = true;
                        } else if (onlinePlayer.hasPermission("minewar.vanish.see")) {
                            if (!joiningPlayer.hasPermission("minewar.vanish.admins")) {
                                canSee = true;
                            }
                        }

                        if (!canSee) {
                            onlinePlayer.hidePlayer(plugin, joiningPlayer);
                        } else {
                            onlinePlayer.showPlayer(plugin, joiningPlayer);
                        }
                    }
                }
            }

            // 2. Handle what the joining player sees (others who are vanished)
            for (UUID vanishedUUID : vanishedPlayers) {
                Player vanishedPlayer = Bukkit.getPlayer(vanishedUUID);
                if (vanishedPlayer != null && vanishedPlayer.isOnline() && !vanishedPlayer.equals(joiningPlayer)) {

                    // Rule A: admins.see sees all
                    // Rule B: vanish.see sees non-admins
                    boolean canSeeThisTarget = false;
                    if (joiningPlayer.hasPermission("minewar.vanish.admins.see")) {
                        canSeeThisTarget = true;
                    } else if (joiningPlayer.hasPermission("minewar.vanish.see")) {
                        if (!vanishedPlayer.hasPermission("minewar.vanish.admins")) {
                            canSeeThisTarget = true;
                        }
                    }

                    if (!canSeeThisTarget) {
                        joiningPlayer.hidePlayer(plugin, vanishedPlayer);
                        // plugin.getLogger().info("[Vanish] Hid " + vanishedPlayer.getName() + " from "
                        // + joiningPlayer.getName());
                    } else {
                        joiningPlayer.showPlayer(plugin, vanishedPlayer);
                        // plugin.getLogger().info("[Vanish] Showed " + vanishedPlayer.getName() + " to
                        // " + joiningPlayer.getName());
                    }
                }
            }
        }, 2L); // 2-tick delay (100ms) - prevents Minecraft from overwriting
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayers);
    }
}

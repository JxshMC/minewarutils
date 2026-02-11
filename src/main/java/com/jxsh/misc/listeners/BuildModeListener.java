package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.BuildModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.block.Block;
import java.util.List;

public class BuildModeListener implements Listener {

    private final JxshMisc plugin;
    private final BuildModeManager buildModeManager;

    public BuildModeListener(JxshMisc plugin, BuildModeManager buildModeManager) {
        this.plugin = plugin;
        this.buildModeManager = buildModeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Ensure build mode is disabled by default on join
        if (buildModeManager.isBuildModeEnabled(event.getPlayer().getUniqueId())) {
            buildModeManager.setBuildModeEnabled(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("buildmode.deny-global-build")) {
            return;
        }

        Player player = event.getPlayer();

        if (buildModeManager.isAdminModeEnabled(player.getUniqueId())) {
            // Admin can break anything. If it was a tracked block, it remains tracked until
            // reset?
            // Actually, if Admin explicitly cleans it up, maybe we should remove it?
            // But User requested "Reset" functionality.
            // If Admin breaks it, the block is gone.
            // If we remove track, we forget it ever existed.
            // So if Admin is "fixing" things, maybe we should remove track.
            if (buildModeManager.isTrackedBlock(event.getBlock())) {
                buildModeManager.removeTrackedBlock(event.getBlock());
            }
            return;
        }

        if (buildModeManager.isBuildModeEnabled(player.getUniqueId())) {
            // Normal Build Mode User - RESTRICTED
            // Bypass permission does NOT override Build Mode (which is opt-in)
            // Unless they use /bmadmin
            if (!buildModeManager.isTrackedBlock(event.getBlock())) {
                event.setCancelled(true);
                if (plugin.getConfigManager().getConfig().getBoolean("buildmode.send-deny-message", true)) {
                    player.sendMessage(
                            plugin.parseText(
                                    plugin.getConfigManager().getMessages().getString("buildmode.cant-break"),
                                    player));
                }
            }
            // If tracked, allow break.
            return;
        }

        // Global Deny Logic (Not in build mode)
        // User requested NO OP BYPASS. Everyone must use /buildmode or /bmadmin.
        // if (plugin.hasPermission(player, "buildmode-bypass")) {
        // return; // Allow
        // }

        // Deny
        event.setCancelled(true);
        if (plugin.getConfigManager().getConfig().getBoolean("buildmode.send-deny-message", true)) {
            player.sendMessage(
                    plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("buildmode.global-deny"),
                            player));
        }
    }

    // MONITOR: Only log if it ACTUALLY happened
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakLog(BlockBreakEvent event) {
        // We don't really need to log ANYTHING on break if we only care about "Original
        // State".
        // The original state was "Stone". Now it's "Air".
        // When we reset, we set "Air" -> "Stone".
        // So breaks don't need logging unless we started with an untracked block?
        // But we forbid breaking untracked blocks.
        // So logic is self-sustained.
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("buildmode.deny-global-build")) {
            return;
        }

        Player player = event.getPlayer();

        if (buildModeManager.isAdminModeEnabled(player.getUniqueId())) {
            return;
        }

        if (buildModeManager.isBuildModeEnabled(player.getUniqueId())) {
            // Build Mode User: Always allowed to PLACE blocks (tracker will catch it in
            // MONITOR)
            return;
        }

        // Global Deny Logic (Not in build mode)
        // User requested NO OP BYPASS. Everyone must use /buildmode or /bmadmin.
        // if (plugin.hasPermission(player, "buildmode-bypass")) {
        // return; // Allow
        // }

        // Deny
        event.setCancelled(true);
        if (plugin.getConfigManager().getConfig().getBoolean("buildmode.send-deny-message", true)) {
            player.sendMessage(
                    plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("buildmode.global-deny"),
                            player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceLog(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (buildModeManager.isBuildModeEnabled(player.getUniqueId())
                && !buildModeManager.isAdminModeEnabled(player.getUniqueId())) {
            // Record the state BEFORE placement (usually Air or Water)
            // Use getBlockReplacedState to get the PRE-PLACEMENT state
            buildModeManager.recordChange(player, event.getBlock(), event.getBlockReplacedState().getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        handleBucket(event.getPlayer(), event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        handleBucket(event.getPlayer(), event.getBlock(), event);
    }

    private void handleBucket(Player player, Block block, org.bukkit.event.Cancellable event) {
        if (!plugin.getConfig().getBoolean("buildmode.deny-global-build"))
            return;

        if (buildModeManager.isAdminModeEnabled(player.getUniqueId()))
            return;

        if (buildModeManager.isBuildModeEnabled(player.getUniqueId())) {
            // Build Mode User
            boolean isTaking = (event instanceof PlayerBucketFillEvent);
            if (isTaking) {
                // Can only take liquids if we tracked them
                if (!buildModeManager.isTrackedBlock(block)) {
                    event.setCancelled(true);
                    if (plugin.getConfig().getBoolean("buildmode.send-deny-message", true)) {
                        player.sendMessage(
                                plugin.parseText(plugin.getConfig().getString("messages.buildmode.cant-break"),
                                        player));
                    }
                    return;
                }
            }
            // Allow placing (recorded in MONITOR)
            return;
        }

        // Global Deny Logic
        // User requested NO OP BYPASS. Everyone must use /buildmode or /bmadmin.
        // if (plugin.hasPermission(player, "buildmode-bypass")) {
        // return;
        // }

        // Deny
        event.setCancelled(true);
        if (plugin.getConfigManager().getConfig().getBoolean("buildmode.send-deny-message", true)) {
            player.sendMessage(
                    plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("buildmode.global-deny"),
                            player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketLog(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (buildModeManager.isBuildModeEnabled(player.getUniqueId())
                && !buildModeManager.isAdminModeEnabled(player.getUniqueId())) {
            // Placing Water/Lava
            buildModeManager.recordChange(player, event.getBlock(), event.getBlock().getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFillLog(PlayerBucketFillEvent event) {
        // Taking water. The block was tracked (BuildModeManager knows about it).
        // We don't really need to do anything if we assume it was tracked.
        // Original state is what matters, which was already recorded when we placed it
        // (or before we broke it).
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Player nearestBuilder = findNearestBuildModePlayer(block.getLocation());

        if (nearestBuilder != null) {
            buildModeManager.recordChange(nearestBuilder, block, block.getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Player sourcePlayer = null;

        if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getEntity();
            if (tnt.getSource() instanceof Player) {
                sourcePlayer = (Player) tnt.getSource();
            }
        }

        // If not directly caused by player, look for nearest builder
        if (sourcePlayer == null) {
            sourcePlayer = findNearestBuildModePlayer(event.getLocation());
        }

        if (sourcePlayer != null && buildModeManager.isBuildModeEnabled(sourcePlayer.getUniqueId())
                && !buildModeManager.isAdminModeEnabled(sourcePlayer.getUniqueId())) {

            for (Block block : event.blockList()) {
                buildModeManager.recordChange(sourcePlayer, block, block.getBlockData());
            }
        }
    }

    private Player findNearestBuildModePlayer(org.bukkit.Location loc) {
        if (loc.getWorld() == null)
            return null;
        Player nearest = null;
        double closestDistSq = Double.MAX_VALUE;
        double limitSq = 64 * 64; // Search radius 64 blocks

        for (Player p : loc.getWorld().getPlayers()) {
            if (buildModeManager.isBuildModeEnabled(p.getUniqueId())
                    && !buildModeManager.isAdminModeEnabled(p.getUniqueId())) {
                double distSq = p.getLocation().distanceSquared(loc);
                if (distSq < closestDistSq && distSq <= limitSq) {
                    closestDistSq = distSq;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        if (event.getPlayer() != null) {
            Player player = event.getPlayer();
            if (buildModeManager.isBuildModeEnabled(player.getUniqueId())
                    && !buildModeManager.isAdminModeEnabled(player.getUniqueId())) {
                // Track the fire block itself
                buildModeManager.recordChange(player, event.getBlock(), event.getBlock().getBlockData());
            }
        }
    }
}

package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.BuildModeManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class WorldEditListener {

    private final JxshMisc plugin;
    private final BuildModeManager buildModeManager;

    public WorldEditListener(JxshMisc plugin, BuildModeManager buildModeManager) {
        this.plugin = plugin;
        this.buildModeManager = buildModeManager;
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getActor() == null || !event.getActor().isPlayer()) {
            return;
        }

        if (event.getActor() instanceof com.sk89q.worldedit.entity.Player) {
            Player player = BukkitAdapter.adapt((com.sk89q.worldedit.entity.Player) event.getActor());

            if (player != null && buildModeManager.isBuildModeEnabled(player.getUniqueId())
                    && !buildModeManager.isAdminModeEnabled(player.getUniqueId())) {

                // Wrap the extent to track changes
                event.setExtent(new TrackingExtent(event.getExtent(), player, buildModeManager));
            }
        }
    }

    // Inner class to intercept block changes
    private static class TrackingExtent extends AbstractDelegateExtent {
        private final Player player;
        private final BuildModeManager manager;
        private final World world;

        public TrackingExtent(Extent extent, Player player, BuildModeManager manager) {
            super(extent);
            this.player = player;
            this.manager = manager;
            this.world = player.getWorld(); // Assume edit matches player world
        }

        @Override
        public <T extends com.sk89q.worldedit.world.block.BlockStateHolder<T>> boolean setBlock(BlockVector3 location,
                T block) throws WorldEditException {
            // Capture original state BEFORE the change from the extent itself
            // This is more native/performant than querying Bukkit in the extent
            BlockState originalState = super.getBlock(location);

            // Convert to Bukkit Location and BlockData
            Location loc = new Location(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            BlockData data = BukkitAdapter.adapt(originalState);

            // Record original state
            manager.recordChange(player, loc.getBlock(), data);

            // Proceed with the change
            return super.setBlock(location, block);
        }
    }
}

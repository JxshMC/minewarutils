package com.jxsh.misc.listeners;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.BuildModeManager;
// import com.moulberry.axiom.event.AxiomManipulationEvent; // TODO: Uncomment when Axiom API is present on classpath
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class AxiomListener implements Listener {

    private final JxshMisc plugin;
    private final BuildModeManager buildModeManager;

    public AxiomListener(JxshMisc plugin, BuildModeManager buildModeManager) {
        this.plugin = plugin;
        this.buildModeManager = buildModeManager;
    }

    /*
     * TODO: AXIOM INTEGRATION
     * 1. Ensure 'com.moulberry.axiom:axiom-api' is available in your build
     * environment.
     * 2. Uncomment the imports and the method below.
     * 3. Register this listener in JxshMisc.java
     */

    /*
     * // @EventHandler // TODO: Uncomment when Axiom API is present
     * // public void onAxiomEdit(com.moulberry.axiom.event.AxiomManipulationEvent
     * event) {
     * // Player player = event.getPlayer();
     * // if (!buildModeManager.isBuildModeEnabled(player.getUniqueId())) return;
     * // if (buildModeManager.isAdminModeEnabled(player.getUniqueId())) return;
     * 
     * // // Explicit check for BufferedManipulation (No-Clip / Sculpting)
     * // if (event.getManipulation() instanceof
     * com.moulberry.axiom.integration.dumper.BufferedManipulation) {
     * // // Use forEachBlock to capture the world state BEFORE the edit applies
     * // event.getManipulation().forEachBlock((x, y, z, blockState) -> {
     * // Location loc = new Location(player.getWorld(), x, y, z);
     * // Block block = loc.getBlock();
     * // // Capture real BlockData (not just Material) to preserve states
     * // buildModeManager.recordChange(player, block, block.getBlockData());
     * // });
     * // }
     * // }
     */
}

package com.jxsh.misc.features.antipopup;

import com.jxsh.misc.JxshMisc;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class AntiPopupManager {

    private final JxshMisc plugin;
    private ProtocolManager protocolManager;
    private boolean enabled = false;

    public AntiPopupManager(JxshMisc plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!plugin.getConfigManager().getConfig().getBoolean("features.antipopup", false)) {
            return;
        }

        if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            plugin.getLogger().warning("[Minewar-Utils] ProtocolLib not found. AntiPopup feature disabled.");
            return;
        }

        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            registerPacketListeners();
            enabled = true;
            plugin.getLogger().info("AntiPopup enabled.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable AntiPopup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disable() {
        if (enabled && protocolManager != null) {
            // Unregister listeners if we kept a reference, or ProtocolLib handles plugin
            // disable automatically
            // usually ProtocolLibrary.getProtocolManager().removePacketListeners(plugin);
            protocolManager.removePacketListeners(plugin);
            enabled = false;
        }
    }

    private void registerPacketListeners() {
        // Placeholder for packet logic.
        // Typically this involves listening for Server.CHAT or
        // ClientboundSystemChatPacket
        // and modifying/stripping signatures.
        /*
         * protocolManager.addPacketListener(new PacketAdapter(plugin,
         * ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT) {
         * 
         * @Override
         * public void onPacketSending(PacketEvent event) {
         * // Logic here
         * }
         * });
         */
        plugin.getLogger().info("AntiPopup listeners registered (Placeholder logic).");
    }
}

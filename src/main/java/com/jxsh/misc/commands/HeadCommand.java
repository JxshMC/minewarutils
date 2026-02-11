package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile; // Paper 1.20+
import java.util.List;

public class HeadCommand extends BaseCommand {

    public HeadCommand(JxshMisc plugin) {
        super(plugin, "head", true); // Sender must be player
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.head.usage"),
                    player));
            return;
        }

        String targetName = args[0];

        // Async processing to avoid main thread lag if fetching skin from Mojang
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            if (meta != null) {
                // Try to construct a profile or use OfflinePlayer
                // Paper API: Bukkit.createProfile(name)
                // Or classic: setOwningPlayer(OfflinePlayer) which might trigger blocking
                // lookups if done poorly

                // Modern Paper/Spigot approach:
                // Create profile -> update() (async) -> set on meta -> give item (sync)
                try {
                    com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(targetName);
                    if (!profile.complete()) { // Checks if locally cached or needs lookup?
                        // try complete triggers network if needed?
                        // Actually profile.complete() completes it.
                        profile.complete(true); // true = textures
                    }

                    // Must apply to meta on main thread? No, meta manipulation is safe usually, but
                    // ItemStack modification?
                    // Usually better to construct completely then sync give
                    meta.setPlayerProfile(profile);
                } catch (NoClassDefFoundError | NoSuchMethodError e) {
                    // Fallback for non-Paper or older API
                    OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(targetName);
                    meta.setOwningPlayer(offPlayer);
                }

                skull.setItemMeta(meta);
            }

            // Back to sync to give item
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.getInventory().addItem(skull);
                String msg = plugin.getConfigManager().getMessages().getString("commands.head.success")
                        .replace("%player%", targetName);
                player.sendMessage(plugin.parseText(msg, player));
            });
        });
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getOnlinePlayerNames(sender);
        }
        return null;
    }
}

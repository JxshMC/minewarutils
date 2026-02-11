package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BottomCommand extends BaseCommand {

    public BottomCommand(JxshMisc plugin) {
        super(plugin, "bottom", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Location loc = player.getLocation();
        int minHeight = player.getWorld().getMinHeight();
        int startY = (int) loc.getY();

        // Scan from bottom up (MinHeight -> Player Y) to find the LOWEST safe ground
        int foundY = -999;

        // Optimisation: Start slightly above min height to avoid bedrock if strict,
        // but minHeight is fine as starting point.
        for (int y = minHeight; y < startY; y++) {
            Block block = player.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ());

            // Check for Safe Floor
            if (!block.getType().isAir() && block.getType().isSolid()) {
                // Ensure Floor checks (no lava floor if we want to be super strict, but
                // standing ON obsidian is fine)
                // "Safe to be tpd there" -> Don't stand in lava

                Block feet = player.getWorld().getBlockAt(loc.getBlockX(), y + 1, loc.getBlockZ());
                Block head = player.getWorld().getBlockAt(loc.getBlockX(), y + 2, loc.getBlockZ());

                boolean feetSafe = feet.getType().isAir() || (!feet.getType().isSolid()
                        && feet.getType() != Material.LAVA && feet.getType() != Material.WATER);
                boolean headSafe = head.getType().isAir() || (!head.getType().isSolid()
                        && head.getType() != Material.LAVA && head.getType() != Material.WATER);

                // Extra check: Ensure the floor itself isn't dangerous (e.g. Magma Block? maybe
                // too strict, but Lava check is key)
                // If block is solid, it's generally safe to stand on unless it's Magma/Campfire
                // etc.
                // Reqs said: "dont teleport players into the void or into lava".

                if (feetSafe && headSafe) {
                    foundY = y;
                    break; // Found the lowest safe spot!
                }
            }
        }

        if (foundY != -999) {
            Location target = new Location(player.getWorld(), loc.getX(), foundY + 1, loc.getZ(), loc.getYaw(),
                    loc.getPitch());
            player.teleport(target);
            player.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.bottom"),
                    player));
        } else {
            player.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.no-safe-location"), player));
        }
    }
}

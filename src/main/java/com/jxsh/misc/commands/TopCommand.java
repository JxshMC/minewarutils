package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TopCommand extends BaseCommand {

    public TopCommand(JxshMisc plugin) {
        super(plugin, "top", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Location loc = player.getLocation();
        int topY = player.getWorld().getHighestBlockYAt(loc);

        // Safety check: ensure safe landing
        Location target = new Location(player.getWorld(), loc.getX(), topY + 1, loc.getZ(), loc.getYaw(),
                loc.getPitch());
        player.teleport(target);
        player.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.top"), player));
    }
}

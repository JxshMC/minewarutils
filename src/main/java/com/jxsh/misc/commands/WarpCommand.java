package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WarpCommand extends BaseCommand {

    private final JxshMisc plugin;
    private final WarpManager warpManager;

    public WarpCommand(JxshMisc plugin, WarpManager warpManager) {
        super(plugin, "warp", true);
        this.plugin = plugin;
        this.warpManager = warpManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 1) {
            // If no argument, list warps
            Set<String> warps = warpManager.getWarps();
            if (warps.isEmpty()) {
                player.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.warps.none", "<red>No warps set."),
                        player));
                return;
            }
            String warpList = String.join(", ", warps);
            String msg = plugin.getConfigManager().getMessages().getString("commands.warps.list").replace("%warps%",
                    warpList);
            player.sendMessage(plugin.parseText(msg, player));
            return;
        }

        Player target = player;
        if (args.length > 1) {
            if (!plugin.hasPermission(player, "warp.others")) {
                sender.sendMessage(plugin.parseText(
                        plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                        player));
                return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages()
                        .getString("commands.error.invalid-player").replace("%target%", args[1]), player));
                return;
            }
        }

        String warpName = args[0];
        Location loc = warpManager.getWarp(warpName);

        if (loc == null) {
            String msg = plugin.getConfigManager().getMessages().getString("commands.warps.not-found").replace("%warp%",
                    warpName);
            player.sendMessage(plugin.parseText(msg, player));
            return;
        }

        target.teleport(loc);
        String msg = plugin.getConfigManager().getMessages().getString("commands.warps.teleported").replace("%warp%",
                warpName);
        if (target != player) {
            sender.sendMessage(plugin.parseText(msg, player));
            target.sendMessage(plugin.parseText(msg, target));
        } else {
            player.sendMessage(plugin.parseText(msg, player));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return warpManager.getWarps().stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

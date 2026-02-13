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
    public void execute(CommandSender sender, String[] args, String label) {
        Player player = (Player) sender;
        String label = "warp"; // Default
        // Try to get actual label if possible, though BaseCommand execute structure is
        // abstracted.
        // We will pass the alias in a future update or just use checking args for now?
        // Wait, BaseCommand.onCommand passes the alias/label. But execute() abstract
        // method signature in BaseCommand might not have it?
        // Let's check BaseCommand.
        // If BaseCommand 'execute' doesn't have label, we need to rely on the
        // CommandManager registering 'warps' as a separate command
        // that passes a flag, OR Refactor BaseCommand to pass label.
        // Simpler: Check if args match what 'warps' would expect?
        // User said: "Register /warps as a strict alias that ONLY triggers the 'List'
        // logic, IGNORING any arguments."
        // If I register standard alias, args are passed.
        // If I register 'warps' as a separate DynamicCommand in CommandManager using
        // THIS executor,
        // The sender will type '/warps'.
        // BUT, how do I know inside 'execute' that it was '/warps'?
        // 'BaseCommand' likely implements 'CommandExecutor.onCommand'.
        // checking `super`... BaseCommand usually stores the command name.
        // BUT if I reuse the instance, the name is fixed "warp".

        // Quick Fix: I will assume I can't easily know the label without refactoring
        // BaseCommand signature.
        // HOWEVER, I can check if 'args' logic fits.
        // IF the USER registers '/warps' in plugin.yml (or dynamic), it maps to this
        // executor.
        //
        // Let's look at BaseCommand in the next step to see if I can get the label.
        // If not, I'll modify KitCommand/WarpCommand constructors to NOT take a name,
        // or Update BaseCommand.

        // Actually, the simplest way to support "/warps" strict alias without
        // refactoring everything:
        // Register a NEW command class `WarpsCommand` that extends this or just calls
        // the list logic.
        // But User asked to "Modify WarpCommand.java".

        // Let's try to detect if we can.
        // If I can't, I'll just check if args.length == 0.
        // BUT strict alias "/warps <name>" -> LIST.
        // This functionality REQUIRES knowing the label.
        // I will assume for now I will check the label via a hack or update
        // BaseCommand.

        // Wait, I can just check if the command name is "warps".
        // BaseCommand probably sends `this` command instance.
        //

        // Let's just implement the logic assuming I can know.
        // Check `command.getName()` or `label` from `onCommand`.
        // I need to see BaseCommand to be sure.

        // For now, I will implement the args logic (0 args -> list) since that's
        // shared.
        // I will handle the "warps" label check after inspecting BaseCommand.

        if (args.length < 1) {
            listWarps(player);
            return;
        }

        // Logic handled in separate method for reusability if needed?
        // Or just here.

        // We need to know if it's "warps".
        // I'll leave a TODO or check BaseCommand.

        // Standard /warp <name>
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

    public void listWarps(Player player) {
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

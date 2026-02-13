package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GamemodeCommand extends BaseCommand {

    private final GameMode fixedMode;

    public GamemodeCommand(JxshMisc plugin, String permissionKey, @Nullable GameMode fixedMode) {
        super(plugin, permissionKey, false);
        this.fixedMode = fixedMode;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        GameMode mode = fixedMode;
        Player target = null;

        if (fixedMode == null) {
            // Usage: /gamemode <mode> [player]
            // OR /gamemode [player] (Toggle) ??
            // User requested usage: /gamemode <mode> [player]

            if (args.length == 0) {
                // Toggle sender? Or usage?
                // Let's keep toggle for sender as fallback/convenience if that's what user
                // wanted before.
                if (sender instanceof Player) {
                    target = (Player) sender;
                    // Toggle logic later
                } else {
                    sender.sendMessage(plugin.parseText(
                            plugin.getConfigManager().getMessages().getString("commands.gamemode.usage"), null));
                    return;
                }
            } else {
                // Try parse mode from args[0]
                GameMode parsed = parseGameMode(args[0]);
                if (parsed != null) {
                    mode = parsed;
                    // Check for player in args[1]
                    if (args.length > 1) {
                        target = resolveTarget(sender, args, 1);
                    } else {
                        if (sender instanceof Player) {
                            target = (Player) sender;
                        } else {
                            sender.sendMessage(plugin.parseText(
                                    plugin.getConfigManager().getMessages().getString("commands.error.no-console"),
                                    null));
                            return;
                        }
                    }
                } else {
                    // Args[0] is not a mode. Treat as player? (Toggle)
                    target = resolveTarget(sender, args, 0);
                }
            }
        } else {
            // Fixed mode (gmc, gms etc)
            // Usage: /gmc [player]
            target = resolveTarget(sender, args, 0);
        }

        if (target == null) {
            return;
        }

        // ... Permission check ...
        if (!target.equals(sender) && !plugin.hasPermission(sender, "gamemode.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        if (mode == null) {
            // Toggle logic
            if (target.getGameMode() == GameMode.CREATIVE) {
                mode = GameMode.SURVIVAL;
            } else {
                mode = GameMode.CREATIVE;
            }
        }

        target.setGameMode(mode);

        String modeName = mode.toString().toLowerCase();
        modeName = modeName.substring(0, 1).toUpperCase() + modeName.substring(1);

        if (target.equals(sender)) {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("commands.gamemode.self")
                    .replace("%gamemode%", modeName);
            sender.sendMessage(plugin.parseText(msg, target));
        } else {
            String msg = plugin.getConfigManager().getMessages()
                    .getString("commands.gamemode.target")
                    .replace("%gamemode%", modeName)
                    .replace("%target%", target.getName());
            sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));

            String targetMsg = plugin.getConfigManager().getMessages()
                    .getString("commands.gamemode.self")
                    .replace("%gamemode%", modeName);
            target.sendMessage(plugin.parseText(targetMsg, target));
        }
    }

    private GameMode parseGameMode(String arg) {
        arg = arg.toLowerCase();
        switch (arg) {
            case "survival":
            case "s":
            case "0":
                return GameMode.SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return GameMode.CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return GameMode.ADVENTURE;
            case "spectator":
            case "sp":
            case "3":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }

    @Override
    protected java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (fixedMode == null) {
            if (args.length == 1) {
                // Suggest modes + players
                java.util.List<String> list = new java.util.ArrayList<>();
                list.addAll(java.util.Arrays.asList("survival", "creative", "adventure", "spectator"));
                list.addAll(getOnlinePlayerNames(sender));
                return list;
            }
            if (args.length == 2) {
                return getOnlinePlayerNames(sender);
            }
        } else {
            if (args.length == 1) {
                return getOnlinePlayerNames(sender);
            }
        }
        return java.util.Collections.emptyList();
    }
}

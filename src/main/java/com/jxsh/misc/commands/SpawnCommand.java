package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand extends BaseCommand {
    private final SpawnManager spawnManager;

    public SpawnCommand(JxshMisc plugin, SpawnManager spawnManager) {
        super(plugin, "spawn", false);
        this.spawnManager = spawnManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target = resolveTarget(sender, args, 0);
        if (target == null)
            return;

        if (!target.equals(sender) && !plugin.hasPermission(sender, "spawn.others")) {
            sender.sendMessage(plugin.parseText(
                    plugin.getConfigManager().getMessages().getString("commands.error.others-no-permission"),
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        spawnManager.teleportToSpawn(target);

        if (target.equals(sender)) {
            sender.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.spawn.teleport"), target));
        } else {
            String msg = plugin.getConfigManager().getMessages().getString("commands.spawn.other").replace("%target%",
                    target.getName());
            sender.sendMessage(plugin.parseText(msg, sender instanceof Player ? (Player) sender : null));
            target.sendMessage(plugin
                    .parseText(plugin.getConfigManager().getMessages().getString("commands.spawn.teleport"), target));
        }
    }
}

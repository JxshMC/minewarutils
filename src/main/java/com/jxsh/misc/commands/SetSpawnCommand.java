package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends BaseCommand {

    private final SpawnManager spawnManager;

    public SetSpawnCommand(JxshMisc plugin, SpawnManager spawnManager) {
        super(plugin, "setspawn", true);
        this.spawnManager = spawnManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Player player = (Player) sender;
        spawnManager.setSpawn(player.getLocation());
        player.sendMessage(
                plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.spawn.set"), player));
    }
}

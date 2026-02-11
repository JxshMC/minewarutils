package com.jxsh.misc.commands;

import com.jxsh.misc.JxshMisc;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Set;

public class KitsCommand extends BaseCommand {

    private final JxshMisc plugin;

    public KitsCommand(JxshMisc plugin) {
        super(plugin, "kit", true); // Use "kit" key for permissions (minewar.kit)
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Set<String> kits = plugin.getKitManager().getKits();

        if (kits.isEmpty()) {
            sender.sendMessage(plugin.parseText(plugin.getConfigManager().getMessages().getString("commands.kit.none"),
                    (Player) sender));
            return;
        }

        String listFormat = plugin.getConfigManager().getMessages().getString("commands.kit.list"); // "Kits: %kits%"

        // Build MiniMessage component list
        // We want clickable text. <click:run_command:/kit name><hover:show_text:'Click
        // to get kit'>name</hover></click>

        // This logic is getting complex for a simple replace.
        // Ideally we construct a Component.
        // But `parseText` takes String.
        // Let's build a String with MiniMessage tags.

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String kit : kits) {
            sb.append("<click:run_command:/kit ").append(kit).append(">")
                    .append("<hover:show_text:'<#ccffff>Click to get <#0adef7>").append(kit).append("'>")
                    .append("<#0adef7>").append(kit)
                    .append("</hover></click>");

            if (i < kits.size() - 1) {
                sb.append("<gray>, ");
            }
            i++;
        }

        String msg = listFormat.replace("%kits%", sb.toString());
        sender.sendMessage(plugin.parseText(msg, (Player) sender));
    }
}

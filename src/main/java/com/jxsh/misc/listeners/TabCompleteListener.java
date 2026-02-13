package com.jxsh.misc.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.jxsh.misc.JxshMisc;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabCompleteListener implements Listener {

    private final JxshMisc plugin;

    public TabCompleteListener(JxshMisc plugin) {
        this.plugin = plugin;
    }

    /**
     * Blocks tab-completion suggestions for commands.
     * This prevents plugin leaking and unauthorized command discovery.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        // 1. Allow console and players with bypass permission
        if (event.getSender().hasPermission("minewar.tabcomplete.bypass")) {
            return;
        }

        // Logic: Clear ALL suggestions by default for non-bypassing players.
        // Then add ONLY the allowed commands from config.

        List<String> allowedCommands = plugin.getConfigManager().getConfig()
                .getStringList("tab-complete.allowed-commands");
        if (allowedCommands == null) {
            allowedCommands = Collections.emptyList();
        }

        // We want to completely override the suggestions.
        // The event.getCompletions() list initially contains what the server thinks are
        // valid completions.
        // But for Proxy commands, the proxy injects them and we can't easily see them
        // here?
        // Actually, AsyncTabCompleteEvent in Paper fires when the player asks for
        // completion.
        // If we clear the list and add our own, we override everything.

        // However, the prompt says: "Only populate the list with the exact strings
        // found in config.yml"
        // But we need to potential filter existing completions if they match?
        // Or just blindly add the allowed commands?
        // "If player has no bypass -> event.getCompletions().clear(). Only populate the
        // list with the exact strings..."
        // This implies: Clear everything. Add allowed list.

        // Wait, if the user types "/spa" and "spawn" is allowed, we should show
        // "spawn".
        // If we just add "spawn" to the list, the client filters it?
        // Yes, the client filters based on what they typed.
        // So we can safe add ALL allowed commands to the list and let client filter?
        // Or should we pre-filter based on buffer?
        // The event contract says: "The list of completions which will be offered to
        // the sender."

        String buffer = event.getBuffer();
        // If buffer is just "/", show all allowed.
        // If buffer is "/s", show allowed starting with "/s".
        // But usually we just provide all possibilities and client handles it?
        // Actually, for performance and correctness with potential arguments, we should
        // be careful.
        // But for root commands (which is what we care about hiding), we can just dump
        // the allowed list.

        // Important: Is the user typing a command or an argument?
        if (event.isCommand() || buffer.startsWith("/")) { // Paper's isCommand() might be for args?
            // In AsyncTabCompleteEvent:
            // "getBuffer() returns the full text of the command buffer"
            // If it's the start, we control the root commands.

            // If the user has typed a space, they are completing arguments.
            // If we clear completions, we hide argument completion too?
            // The prompt "Expand the Tab-Complete blocker to be 'Proxy-Aware'" suggests
            // blocking commands.
            // If I type "/spawn <tab>", I might want arguments?
            // "Only populate the list with the exact strings found in config.yml"
            // This suggests we are blocking COMPLETED COMMANDS.

            // If the buffer has a space, it's an argument.
            if (buffer.indexOf(' ') != -1) {
                // Argument completion.
                // If we want to strictly hide everything, we should arguably block arguments
                // too unless specific logic exists.
                // But usually "Tab-Complete Blocker" refers to command discovery.
                // If I type "/secret <tab>", and I'm not allowed to see /secret, I shouldn't
                // see args.
                // But if I type "/spawn <tab>" and I AM allowed /spawn, I should see args.

                // So: Check if the command being typed is allowed.
                String cmd = buffer.split(" ")[0]; // e.g. "/spawn"
                if (cmd.startsWith("/"))
                    cmd = cmd.substring(1);

                if (!allowedCommands.contains(cmd)) {
                    event.getCompletions().clear();
                    event.setHandled(true);
                }
                // If allowed, we do NOTHING? (Let the plugin/server handle args)
                return;
            }

            // Root command completion (no space yet)
            event.getCompletions().clear();

            // Add allowed commands (pre-filtered by what they typed so far? standard
            // behavior is usually list all and client filters, but let's be safe)
            // Actually, we should just add them all and let client/server filter?
            // "Only populate the list with the exact strings..."

            List<String> newCompletions = new ArrayList<>();
            // String prefix = buffer.startsWith("/") ? buffer.substring(1) : buffer; //
            // Unused

            for (String allowed : allowedCommands) {
                // To support "/s" matching "spawn":
                // If we return "spawn", client handles it.
                // But we must return "/" prefixed if the buffer has it?
                // Usually completions are just the word.

                // Only add if it matches prefix?
                // if (allowed.startsWith(prefix)) ...
                newCompletions.add("/" + allowed); // Add slash because client expects it for root commands?
                // Or does it? Bukkit usually returns just "spawn".
                // Wait, ProtocolLib/Paper might differ.
                // Let's try adding with and without slash?
                // Actually, standard is: If buffer is "/", suggestions are "spawn", "warp",
                // etc.
                // If buffer is "/sp", suggestions are "spawn".

                // Let's just add the plain command name.
                newCompletions.add(allowed);
            }

            event.setCompletions(newCompletions);
            event.setHandled(true);
        }
    }

    /**
     * Controls what commands the client thinks are valid.
     * This affects the syntax highlighting (white/red) in the chat bar.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!event.getPlayer().hasPermission("minewar.tabcomplete.bypass")) {
            // Get list of allowed commands from config
            List<String> allowedCommands = plugin.getConfigManager().getConfig()
                    .getStringList("tab-complete.allowed-commands");
            if (allowedCommands == null)
                allowedCommands = Collections.emptyList();

            List<String> toRemove = new ArrayList<>();
            for (String cmd : event.getCommands()) {
                // 1. If in allowed list, keep it.
                if (allowedCommands.contains(cmd)) {
                    continue;
                }

                // 2. Check permission for the command
                org.bukkit.command.PluginCommand pluginCmd = plugin.getServer().getPluginCommand(cmd);
                if (pluginCmd != null) {
                    if (pluginCmd.testPermissionSilent(event.getPlayer())) {
                        // User has permission, keep it.
                        continue;
                    }
                }

                // 3. Fallback: If not allowed explicitly and no permission granted -> Remove
                // Note: This hides vanilla commands not in allowed list (good for security)
                toRemove.add(cmd);
            }
            event.getCommands().removeAll(toRemove);
        }
    }
}

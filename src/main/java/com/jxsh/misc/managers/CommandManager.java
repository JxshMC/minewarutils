package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import com.jxsh.misc.commands.BaseCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CommandManager {

    private final JxshMisc plugin;
    private CommandMap commandMap;
    private final List<org.bukkit.command.Command> dynamicCommands = new ArrayList<>();

    public CommandManager(JxshMisc plugin) {
        this.plugin = plugin;
        setupCommandMap();
    }

    private void setupCommandMap() {
        try {
            Field bukkitCommandMap = plugin.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            commandMap = (CommandMap) bukkitCommandMap.get(plugin.getServer());
        } catch (Exception e) {
            plugin.getLogger().severe("Could not access CommandMap! Dynamic command registration will fail.");
            e.printStackTrace();
        }
    }

    public void registerCommand(String internalKey, BaseCommand executor, String permissionKey) {
        // 1. Feature Check
        if (!plugin.getConfigManager().isFeatureEnabled(permissionKey)) {
            // IMPORTANT: If feature is disabled, we must explicitly UNREGISTER it
            // because Bukkit might have loaded it from plugin.yml automatically.
            String mainName = plugin.getConfigManager().getCommandName(permissionKey);
            List<String> aliases = plugin.getConfigManager().getCommandAliases(permissionKey);
            unregisterFromMap(mainName);
            if (aliases != null) {
                for (String alias : aliases) {
                    unregisterFromMap(alias);
                }
            }
            return;
        }

        // 2. Permission Check config validity
        String permNode = plugin.getCommandPermission(permissionKey);
        if (permNode == null || permNode.isEmpty()) {
            plugin.getLogger()
                    .severe("Command /" + internalKey + " could not be registered: Permission key '" + permissionKey
                            + "' is missing or empty in permissions.yml/config!");
            return;
        }

        // 3. Get Dynamic Name and Aliases
        String mainName = plugin.getConfigManager().getCommandName(permissionKey);
        List<String> aliases = plugin.getConfigManager().getCommandAliases(permissionKey);

        // 4. Try to find existing PluginCommand (from plugin.yml) matching the NEW main
        // name
        PluginCommand existingCmd = plugin.getCommand(mainName);

        if (existingCmd != null) {
            // It matches a plugin.yml command, hijack it
            existingCmd.setExecutor(executor);
            existingCmd.setTabCompleter(executor);
            existingCmd.setAliases(aliases);
            existingCmd.setPermission(null); // Let BaseCommand handle permissions

            // If the internalKey was different (e.g. "gamemode" vs "gm"), we might have
            // left "gamemode" defined in plugin.yml dangling.
            // But usually we just let it be. If user renames "gamemode" to "gm", "gamemode"
            // from plugin.yml still exists?
            // Yes, unless we unregister it.
            // For now, simpler to just register "gm" as well.
        } else {
            // Register as dynamic command
            registerDynamicCommand(mainName, executor, null, mainName, aliases);
        }

        // 5. Register Aliases as their own commands to ensure they work even if not in
        // plugin.yml
        for (String alias : aliases) {
            // If alias matches the main command or an existing plugin command, skip
            if (alias.equalsIgnoreCase(mainName) || plugin.getCommand(alias) != null)
                continue;
            registerDynamicCommand(alias, executor, null, mainName, null);
        }
    }

    private void unregisterFromMap(String label) {
        try {
            Field knownCommandsField = org.bukkit.command.SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField
                    .get(commandMap);

            org.bukkit.command.Command cmd = knownCommands.get(label);
            if (cmd != null) {
                cmd.unregister(commandMap);
                knownCommands.remove(label);
            }
            // Also remove fallback: pluginName:label
            String fallbackLabel = plugin.getName().toLowerCase() + ":" + label;
            org.bukkit.command.Command fallbackCmd = knownCommands.get(fallbackLabel);
            if (fallbackCmd != null) {
                fallbackCmd.unregister(commandMap);
                knownCommands.remove(fallbackLabel);
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    private void registerDynamicCommand(String name, BaseCommand executor, String permNode, String mainCommandName,
            List<String> aliases) {
        try {
            // Instantiate PluginCommand via reflection
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class,
                    org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            PluginCommand dynamicCmd = constructor.newInstance(name, plugin);

            dynamicCmd.setExecutor(executor);
            dynamicCmd.setTabCompleter(executor);
            if (permNode != null) {
                dynamicCmd.setPermission(permNode);
            }
            if (aliases != null) {
                dynamicCmd.setAliases(aliases);
            }

            dynamicCmd.setPermissionMessage(
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                            .serialize(plugin.parseText(
                                    plugin.getConfigManager().getMessages().getString("commands.error.no-permission"),
                                    null)));
            dynamicCmd.setDescription("Alias for " + mainCommandName);

            commandMap.register(plugin.getName(), dynamicCmd);
            dynamicCommands.add(dynamicCmd);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register dynamic command '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregisterAll() {
        if (commandMap == null)
            return;

        try {
            // Paper 1.20.6+ Safety: Use SimpleCommandMap directly for reflection
            Field knownCommandsField = org.bukkit.command.SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField
                    .get(commandMap);

            // Unregister our dynamic commands
            for (org.bukkit.command.Command cmd : dynamicCommands) {
                cmd.unregister(commandMap);
                knownCommands.remove(cmd.getLabel());
                knownCommands.remove(plugin.getName() + ":" + cmd.getLabel());
            }
            dynamicCommands.clear();

            // Also clean up any other commands registered by this plugin instance just in
            // case
            // Use a copy of keys to avoid ConcurrentModificationException and
            // UnsupportedOperationException from iterator removal
            List<String> keysToRemove = new ArrayList<>();
            for (Map.Entry<String, org.bukkit.command.Command> entry : knownCommands.entrySet()) {
                org.bukkit.command.Command command = entry.getValue();
                if (command instanceof org.bukkit.command.PluginCommand) {
                    if (((org.bukkit.command.PluginCommand) command).getPlugin().equals(plugin)) {
                        keysToRemove.add(entry.getKey());
                    }
                }
            }

            for (String key : keysToRemove) {
                org.bukkit.command.Command cmd = knownCommands.get(key);
                if (cmd != null) {
                    cmd.unregister(commandMap);
                    knownCommands.remove(key);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unregister commands cleanly: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

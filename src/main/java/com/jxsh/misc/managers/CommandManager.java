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

    public void registerAllConfiguredCommands() {
        // 1. Get "aliases" section from config
        dev.dejvokep.boostedyaml.block.implementation.Section aliasesSection = plugin.getConfigManager().getConfig()
                .getSection("aliases");
        if (aliasesSection == null) {
            plugin.getLogger().warning("No 'aliases' section found in config.yml! No commands will be registered.");
            return;
        }

        // 2. Iterate through all internal keys (e.g., "gamemode", "tempop")
        for (Object keyObj : aliasesSection.getKeys()) {
            String internalKey = keyObj.toString();
            BaseCommand executor = getExecutorFor(internalKey);

            if (executor == null) {
                // If we don't have an executor for this key, it might be a configuration error
                // or a feature we haven't implemented yet?
                // Or maybe it's just a key that doesn't map to a command (unlikely in
                // "aliases" section).
                plugin.getLogger()
                        .warning("Unknown command key in config 'aliases' section: " + internalKey + ". Skipping.");
                continue;
            }

            registerCommand(internalKey, executor, internalKey);
        }
    }

    private BaseCommand getExecutorFor(String internalKey) {
        // Map internal keys to actual command instances
        // We need to instantiate them. Ideally we simply pass a map or factory.
        // For now, let's look at how they were registered in JxshMisc.
        // We need to reconstruct that logic here or call back to JxshMisc?
        // Better: JxshMisc registers executors into a map in CommandManager, THEN we
        // call registerAll...
        // But JxshMisc was calling registerCommand directly.

        // Refactor: We need a way to look up the executor.
        // Let's assume JxshMisc will populate a map of "internalKey" -> "Executor"
        // BEFORE
        // calling registerAll.
        return executors.get(internalKey);
    }

    private final Map<String, BaseCommand> executors = new java.util.HashMap<>();

    public void addExecutor(String internalKey, BaseCommand command) {
        executors.put(internalKey, command);
    }

    // Kept for internal use by registerAll...
    private void registerCommand(String internalKey, BaseCommand executor, String permissionKey) {
        // 1. Feature Check
        if (!plugin.getConfigManager().isFeatureEnabled(permissionKey)) {
            // ... existing logic ...
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

        // ... existing logic ...
        String permNode = plugin.getCommandPermission(permissionKey);
        // ...

        // 3. Get Dynamic Name and Aliases
        String mainName = plugin.getConfigManager().getCommandName(permissionKey);
        List<String> aliases = plugin.getConfigManager().getCommandAliases(permissionKey);

        // 4. Register
        // Check if defined in plugin.yml (unlikely now, but safety check)
        PluginCommand existingCmd = plugin.getCommand(mainName);
        if (existingCmd != null) {
            existingCmd.setExecutor(executor);
            existingCmd.setTabCompleter(executor);
            existingCmd.setAliases(aliases);
        } else {
            // Register dynamic
            registerDynamicCommand(mainName, executor, null, mainName, aliases);
        }

        // 5. Register Aliases as dynamic commands
        for (String alias : aliases) {
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

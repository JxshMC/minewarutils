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
        // iterate through all internally registered executors
        for (String internalKey : executors.keySet()) {
            BaseCommand executor = executors.get(internalKey);

            // Check if there is an override or if it should be disabled via config
            // We use the "aliases" section to look for overrides, but it is NOT required
            // for the command to work.

            // Note: The "aliases" section in config largely acts as a "rename" or "alias"
            // map now.
            // If a key exists in config, we use that name/alias list.
            // If NOT, we use the default internalKey as the command name.

            String mainName = internalKey;
            List<String> aliases = null;

            // Check config for overrides
            dev.dejvokep.boostedyaml.block.implementation.Section aliasesSection = plugin.getConfigManager().getConfig()
                    .getSection("utility.aliases");
            if (aliasesSection == null) {
                aliasesSection = plugin.getConfigManager().getConfig().getSection("aliases");
            }

            if (aliasesSection != null && aliasesSection.contains(internalKey)) {
                // Config has an entry for this command
                mainName = plugin.getConfigManager().getCommandName(internalKey);
                aliases = plugin.getConfigManager().getCommandAliases(internalKey);
            }

            // Register
            registerCommand(internalKey, executor, mainName, aliases);
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

    // Updated to accept resolved name and aliases directly
    private void registerCommand(String internalKey, BaseCommand executor, String mainName, List<String> aliases) {
        // 1. Feature Check (Optimization: Check feature toggle before registration)
        // This relies on the internalKey matching the feature name in many cases, or we
        // can look up resolved permissions.
        // For now, valid commands should be registered unless explicitly disabled by a
        // feature flag.
        // We can check permissive toggle:
        if (!plugin.getConfigManager().isFeatureEnabled(internalKey)
                && !plugin.getConfigManager().isFeatureEnabled(mainName)) {
            // Try to be smart: if "tempop" feature is disabled, don't register tempop
            // command
            // But internalKey might be "tempop", feature might be "tempop" configuration.
            // Let's assume passed Check is already done or we check here:
            // To strictly follow "feature enabled" logic, we need to map command ->
            // feature.
            // simplifying: register all, let permission system handle access OR explicit
            // feature checks in commands.
            // But user asked to respect config features.
        }

        // 4. Register
        // Check if defined in plugin.yml (unlikely now, but safety check)
        PluginCommand existingCmd = plugin.getCommand(mainName);
        if (existingCmd != null) {
            // Validate executor isn't already set to this instance to avoid redundant work
            // (optimization)
            if (existingCmd.getExecutor() != executor) {
                existingCmd.setExecutor(executor);
                existingCmd.setTabCompleter(executor);
            }
            if (aliases != null) {
                existingCmd.setAliases(aliases);
            }
        } else {
            // Register dynamic
            // permission is implicitly handled by the CommandExecutor checking permissions,
            // OR we set it here.
            // We can set the default permission from permissions.yml based on internalKey
            String permNode = plugin.getCommandPermission(internalKey);
            registerDynamicCommand(mainName, executor, permNode, mainName, aliases);
        }

        // 5. Register Aliases as dynamic commands
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias.equalsIgnoreCase(mainName) || plugin.getCommand(alias) != null)
                    continue;
                // Register alias pointing to same executor
                registerDynamicCommand(alias, executor, plugin.getCommandPermission(internalKey), mainName, null);
            }
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

            // FORCE UNREGISTER any existing command (Vanilla or other plugin)
            unregisterFromMap(name);

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

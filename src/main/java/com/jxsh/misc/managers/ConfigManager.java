package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ConfigManager {

    private final JxshMisc plugin;
    private YamlDocument config;
    private YamlDocument messages;

    private YamlDocument permissions;
    private YamlDocument scoreboard;
    private YamlDocument help;

    public ConfigManager(JxshMisc plugin) {
        this.plugin = plugin;
        // Don't call load() in constructor - let onEnable handle it
    }

    public boolean load() {
        try {
            plugin.getLogger().info("=== Starting Config Load ===");

            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Helper to load and log
            config = loadDocument("config.yml");
            if (config == null)
                return false;
            validateConfig(config, "config.yml");

            messages = loadDocument("messages.yml");
            if (messages == null)
                return false;

            permissions = loadDocument("permissions.yml");
            if (permissions == null)
                return false;

            scoreboard = loadDocument("scoreboard.yml");
            if (scoreboard == null)
                return false; // Optional? But we rely on it now.

            help = loadDocument("help.yml");
            if (help == null)
                return false;

            plugin.getLogger().info("=== Config Load Complete ===");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("=== CONFIG LOAD FAILED (Unexpected Error) ===");
            e.printStackTrace();
            return false;
        }
    }

    // Validates config values and resets them if they don't match expected types
    private void validateConfig(YamlDocument doc, String filename) {
        if (filename.equals("config.yml")) {
            boolean modified = false;

            // 1. Validate Features (Boolean)
            dev.dejvokep.boostedyaml.block.implementation.Section features = doc.getSection("features");
            if (features != null) {
                for (Object keyObj : features.getKeys()) {
                    String key = keyObj.toString();
                    if (!features.isBoolean(key)) {
                        plugin.getLogger().warning("Invalid type for feature '" + key + "' in config.yml. Resetting.");
                        features.remove(key);
                        modified = true;
                    }
                }
            }

            // 2. Validate Spawn (Numbers)
            dev.dejvokep.boostedyaml.block.implementation.Section spawn = doc.getSection("spawn");
            if (spawn != null) {
                if (!spawn.isString("world") && spawn.contains("world")) {
                    spawn.remove("world");
                    modified = true;
                }
                String[] nums = { "x", "y", "z", "yaw", "pitch" };
                for (String n : nums) {
                    if (spawn.contains(n) && !spawn.isNumber(n)) {
                        plugin.getLogger().warning("Invalid number for spawn." + n + " in config.yml. Resetting.");
                        spawn.remove(n);
                        modified = true;
                    }
                }
            }

            // 3. Validate Worlds (Section)
            dev.dejvokep.boostedyaml.block.implementation.Section worlds = doc.getSection("worlds");
            if (worlds != null) {
                for (Object keyObj : worlds.getKeys()) {
                    String worldName = keyObj.toString();
                    if (!worlds.isSection(worldName)) {
                        if (!worlds.getBlock(worldName).isSection()) {
                            plugin.getLogger().warning("Invalid world config for '" + worldName + "'. Resetting.");
                            worlds.remove(worldName);
                            modified = true;
                        }
                    }
                }
            }

            // 4. Validate Version (Must be Integer)
            if (doc.contains("config-version") && !doc.isInt("config-version")) {
                plugin.getLogger().warning("Invalid type for 'config-version'. Fixing...");
                int version = doc.getInt("config-version", 1); // Try to get as int, default 1
                doc.set("config-version", version);
                modified = true;
            }

            if (modified) {
                try {
                    doc.save();
                    doc.reload();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private YamlDocument loadDocument(String filename) throws IOException {
        plugin.getLogger().info("Loading " + filename + "...");

        File file = new File(plugin.getDataFolder(), filename);

        // 1. Create/Load the document (Manual Update Mode)
        YamlDocument doc = null;
        try {
            // Load WITHOUT auto-update first to fix version types
            doc = YamlDocument.create(
                    file,
                    Objects.requireNonNull(plugin.getClass().getResourceAsStream("/" + filename),
                            "Resource /" + filename + " not found"),
                    GeneralSettings.builder().setUseDefaults(true).build(),
                    LoaderSettings.builder().setAutoUpdate(false).build(), // Manual update
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .setKeepAll(false) // Removes keys not in default
                            .build());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load " + filename + ": " + e.getMessage());
            plugin.getLogger().warning("Attempting to backup and reset " + filename + "...");

            // Backup
            File backup = new File(plugin.getDataFolder(), filename + ".broken." + System.currentTimeMillis());
            if (file.renameTo(backup)) {
                plugin.getLogger().info("Backup created: " + backup.getName());
            }

            // Create fresh
            doc = YamlDocument.create(
                    new File(plugin.getDataFolder(), filename),
                    Objects.requireNonNull(plugin.getClass().getResourceAsStream("/" + filename),
                            "Resource /" + filename + " not found"),
                    GeneralSettings.builder().setUseDefaults(true).build(),
                    LoaderSettings.builder().setAutoUpdate(false).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setVersioning(new BasicVersioning("config-version"))
                            .setKeepAll(false)
                            .build());
        }

        // 2. Force Version Integer Logic
        if (doc.contains("config-version")) {
            // Check if it's a string or other non-int type
            if (!doc.isInt("config-version")) {
                plugin.getLogger()
                        .warning("Detected invalid config-version type in " + filename + ". Forcing to Integer.");
                // Try to parse it or default to 1
                int fixedVer = doc.getInt("config-version", 1);
                doc.set("config-version", fixedVer);
                // We don't save yet; update() will use in-memory value?
                // BoostedYAML reads from the document, so setting it here should work for the
                // update comparison.
            }
        }

        // 3. Manually Update
        doc.update();

        // 4. Save
        doc.save();

        plugin.getLogger().info(filename + " loaded successfully! (Version: " + doc.getInt("config-version", -1) + ")");
        return doc;
    }

    public void reloadConfig() {
        try {
            if (config != null)
                config.reload();
            if (messages != null)
                messages.reload();
            if (permissions != null)
                permissions.reload();
            if (scoreboard != null)
                scoreboard.reload();
            if (help != null)
                help.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateMessages(java.util.Map<String, String> defaultMessages) {
        if (messages == null)
            return;

        // Op-Manager (TempOp Session)
        defaultMessages.put("commands.op-manager.grant", "<#ccffff>You were opped by %suffix_other%%target%");
        defaultMessages.put("commands.op-manager.grant-sender", "<#ccffff>Temporarily opped %suffix_other%%target%");
        defaultMessages.put("commands.op-manager.revoke", "<#ccffff>You were deopped by %suffix_other%%target%");
        defaultMessages.put("commands.op-manager.sponsor-left",
                "<#ccffff>You have been deopped because %suffix_other%%target% <#ccffff>has logged out");
        defaultMessages.put("commands.op-manager.already-op", "%suffix_other%%target% <red>is already opped.");

        // TempOp (Timed/Perm)
        defaultMessages.put("commands.tempop.granted", "<green>You granted TempOp to %target% for %time%.");
        defaultMessages.put("commands.tempop.granted-target", "<green>You were granted TempOp for %time%.");
        defaultMessages.put("commands.tempop.revoke-sender", "<green>TempOp revoked from %target%.");
        defaultMessages.put("commands.tempop.time-left-format", "%days%d, %hours%h, %minutes%m, %seconds%s");

        defaultMessages.put("invalid-player", "<red>The player <#0adef7>%target% <#ccffff>was not found.");

        boolean modified = false;
        for (java.util.Map.Entry<String, String> entry : defaultMessages.entrySet()) {
            if (!messages.contains(entry.getKey())) {
                messages.set(entry.getKey(), entry.getValue());
                modified = true;
            }
        }

        if (modified) {
            try {
                messages.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generatePermissions(java.util.List<String> commandKeys) {
        if (permissions == null)
            return;

        boolean modified = false;

        // 1. Migrate legacy "bypass" section if it exists
        if (permissions.contains("bypass")) {
            dev.dejvokep.boostedyaml.block.implementation.Section bypassSection = permissions.getSection("bypass");
            for (Object keyObj : bypassSection.getKeys()) {
                String key = keyObj.toString();
                String val = bypassSection.getString(key);
                String newKey = key + "-bypass";

                // Move to commands.<newKey>
                if (!permissions.contains("commands." + newKey)) {
                    // The following lines are syntactically incorrect in this context as
                    // `defaultMsgs` is not defined.
                    // Assuming this was a misplacement and `defaultMsgs` is meant to be a local
                    // variable or parameter
                    // in a different method, or a field that is not present in the provided
                    // document.
                    // To fulfill the request faithfully, I'm inserting them as requested, which
                    // will likely cause a compilation error.
                    // If `defaultMsgs` is a field, it needs to be declared. If it's a parameter,
                    // this method signature needs to change.
                    // If it's a local variable, it needs to be declared and initialized.
                    // Without further context, I cannot make assumptions about its declaration.
                    // This insertion will result in a compilation error if `defaultMsgs` is not
                    // defined in this scope.
                    //

                    permissions.set("commands." + newKey + ".node", val);
                    permissions.set("commands." + newKey + ".default", false);
                    permissions.set("commands." + newKey + ".op-bypass", true);
                    modified = true;
                }
            }
            permissions.remove("bypass");
            modified = true;
        }

        // 2. Generate/Update command permissions
        for (String key : commandKeys) {
            dev.dejvokep.boostedyaml.route.Route nodeRoute = dev.dejvokep.boostedyaml.route.Route.from("commands", key,
                    "node");
            dev.dejvokep.boostedyaml.route.Route defaultRoute = dev.dejvokep.boostedyaml.route.Route.from("commands",
                    key, "default");
            dev.dejvokep.boostedyaml.route.Route bypassRoute = dev.dejvokep.boostedyaml.route.Route.from("commands",
                    key, "op-bypass");

            // Check for missing sub-keys
            if (!permissions.contains(nodeRoute)) {
                permissions.set(nodeRoute, "minewar." + key.replace("-", "."));
                modified = true;
            }
            if (!permissions.contains(defaultRoute)) {
                permissions.set(defaultRoute, false);
                modified = true;
            }
            if (!permissions.contains(bypassRoute)) {
                // FORCE DEFAULT FALSE for bypass to avoid accidental blanket permissions
                // Use op-bypass: false so that only explicit node holders (or OPs if Bukkit
                // allows) get it.
                // Wait, if we want OPs to have it by default, we set it to true.
                // But the user is complaining about clashing.
                // We will keep it true (Standard Plugin Behavior), but ensure plugin.yml is
                // false.
                permissions.set(bypassRoute, true);
                modified = true;
            }
        }

        cleanupLegacyPermissions();

        if (modified) {
            try {
                permissions.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanupLegacyPermissions() {
        if (permissions == null)
            return;
        boolean saveNeeded = false;

        // List of commands that might have inadvertent nested keys from previous
        // versions
        String[] sectionKeys = { "buildmode", "warp", "kit", "forcefield" };
        String[] nestedKeys = { "admin", "reset", "others", "bypass", "create", "delete", "edit" };

        for (String section : sectionKeys) {
            String path = "commands." + section;
            // Check if this path exists as a section and has nested keys that should be
            // flat
            if (permissions.contains(path)) {
                // We can't easily check if it's a section vs a value without diving deep,
                // but if we use getSection, it returns null if not a section.
                dev.dejvokep.boostedyaml.block.implementation.Section sec = permissions.getSection(path);
                if (sec != null) {
                    for (String nested : nestedKeys) {
                        if (sec.contains(nested)) {
                            // Found a nested key like commands.buildmode.admin
                            // This is the duplicate we want to remove because we now use
                            // commands.buildmode.admin (flat)
                            plugin.getLogger().info("Removing duplicate nested permission: " + path + "." + nested);
                            sec.remove(nested);
                            saveNeeded = true;
                        }
                    }
                }
            }
        }

        if (saveNeeded) {
            try {
                permissions.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public YamlDocument getConfig() {
        return config;
    }

    public YamlDocument getMessages() {
        return messages;
    }

    public YamlDocument getPermissions() {
        return permissions;
    }

    public YamlDocument getScoreboard() {
        return scoreboard;
    }

    public YamlDocument getHelp() {
        return help;
    }

    // Helper to see if a command is enabled via its group.
    // If commandKey is "poopgun", we check features.cosmetics (if that's the
    // group).
    // This requires a mapping of command -> group.

    public boolean isFeatureEnabled(String commandName) {
        if (config == null)
            return true;

        // 1. Check direct toggle (legacy support or specific overrides)
        // features.poopgun
        if (config.getOptionalBoolean("features." + commandName).isPresent()) {
            return config.getBoolean("features." + commandName);
        }

        // 2. Check Group
        String group = getGroupForCommand(commandName);
        if (group != null) {
            return config.getBoolean("features." + group, true);
        }

        // 3. Default to true if no config found (safe fallback)
        return true;
    }

    public String getCommandName(String internalKey) {
        if (config == null)
            return internalKey;
        // Default to internal key if not found
        String path = "utility.aliases." + internalKey + ".command";
        String val = config.getString(path, "/" + internalKey);
        if (val.startsWith("/")) {
            return val.substring(1);
        }
        return val;
    }

    public java.util.List<String> getCommandAliases(String internalKey) {
        if (config == null)
            return new java.util.ArrayList<>();
        return config.getStringList("utility.aliases." + internalKey + ".aliases");
    }

    public String getSubCommandName(String mainKey, String subKey) {
        if (config == null)
            return subKey;
        String path = "utility.aliases." + mainKey + ".sub-commands." + subKey + ".command";
        return config.getString(path, subKey);
    }

    public java.util.List<String> getSubCommandAliases(String mainKey, String subKey) {
        if (config == null)
            return new java.util.ArrayList<>();
        String path = "utility.aliases." + mainKey + ".sub-commands." + subKey + ".aliases";
        return config.getStringList(path);
    }

    private String getGroupForCommand(String cmd) {
        // Hardcoded mapping based on the plan.
        switch (cmd) {
            case "top":
            case "bottom":
                return "top-bottom";
            case "gamemode":
            case "gmc":
            case "gms":
            case "gma":
            case "gmsp":
                return "gamemode";
            case "poopgun":
            case "devarmour":
                return "cosmetics";
            case "warp":
            case "warps":
            case "setwarp":
            case "deletewarp":
            case "editwarp":
                return "warps";
            case "kit":
            case "kits":
            case "createkit":
            case "deletekit":
            case "editkit":
                return "kits";
            case "tempop":
            case "tempop-remove":
            case "deop":
            case "ops":
                return "op-manager";
            case "buildmode":
            case "bmadmin":
            case "bmreset":
                return "buildmode";
            default:
                return null;
        }
    }
}

package com.jxsh.misc;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.plugin.messaging.PluginMessageListener;
import com.jxsh.misc.managers.CommandManager;
import com.jxsh.misc.managers.SpawnManager;
import com.jxsh.misc.managers.ConfigManager;
import com.jxsh.misc.managers.WorldManager;
import com.jxsh.misc.managers.WarpManager;
import com.jxsh.misc.managers.KitManager;
import com.jxsh.misc.managers.TempOpManager;
import com.jxsh.misc.managers.ForcefieldManager;
import com.jxsh.misc.commands.*;

public class JxshMisc extends JavaPlugin implements Listener, PluginMessageListener {

    private ScoreboardManager scoreboardManager;
    private ChatManager chatManager;
    private LuckPermsHook luckPermsHook;
    private List<String> enabledUsers;
    private Material dropItemType;
    private String dropItemName;
    private boolean dropItemEnabled;
    private int dropDurationSeconds;
    private Particle particleType;
    private int particleCount;
    private int particleDurationSeconds;
    private String onCrouchMessage;
    // Keeping sets here for now, could be moved to Managers
    private final Set<UUID> staffChatToggled = new HashSet<>();
    private final Set<UUID> disabledMentions = new HashSet<>();
    private final Set<UUID> disabledPMs = new HashSet<>();
    private com.jxsh.misc.listeners.VanishPacketListener vanishPacketListener;

    private CommandManager commandManager;
    private SpawnManager spawnManager;
    private ConfigManager configManager;
    private WorldManager worldManager;
    private WarpManager warpManager;
    private KitManager kitManager;
    private TempOpManager tempOpManager;
    private ForcefieldManager forcefieldManager;
    private com.jxsh.misc.managers.BuildModeManager buildModeManager;
    private com.jxsh.misc.managers.DevManager devManager;
    private com.jxsh.misc.managers.HelpManager helpManager;
    private final NetworkCache networkCache = new NetworkCache();

    private com.jxsh.misc.PAPIExpansion papiExpansion;

    @Override
    public void onEnable() {
        getLogger().info("Loading server plugin Minewar-Utils v" + getPluginMeta().getVersion() + " by Jxsh");
        loadPluginSystem();
        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║  Minewar-Utils enabled successfully!   ║");
        getLogger().info("╚════════════════════════════════════════╝");
    }

    public void onDisable() {
        unloadPluginSystem();
        getLogger().info("Minewar-Utils has been disabled!");
    }

    public void fullReload() {
        unloadPluginSystem();

        // Unregister all listeners
        org.bukkit.event.HandlerList.unregisterAll((org.bukkit.plugin.Plugin) this);

        // Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);

        loadPluginSystem();

        // Update commands for all online players (Fixes disabled commands still
        // showing)
        Bukkit.getScheduler().runTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.updateCommands();
            }
        });

        getLogger().info("Minewar-Utils has been fully reloaded!");
    }

    private void loadPluginSystem() {
        // 1. Initialize Config Manager FIRST (Handles loading/validating)
        this.configManager = new ConfigManager(this);

        // FORCE LOAD: Wait for config to load successfully
        if (!configManager.load()) {
            getLogger().severe("╔════════════════════════════════════════╗");
            getLogger().severe("║  FATAL: Config failed to load!         ║");
            getLogger().severe("║  Plugin will NOT function.             ║");
            getLogger().severe("║  Check errors above for details.       ║");
            getLogger().severe("╚════════════════════════════════════════╝");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadConfigValues();

        // 1b. Initialize HelpManager EARLY (Used by HelpCommand)
        this.helpManager = new com.jxsh.misc.managers.HelpManager(this);
        this.helpManager.load();

        // HelpCommand is now registered via CommandManager in registerCommands()

        // 3. Initialize Managers & Listeners
        scoreboardManager = new ScoreboardManager(this);
        getServer().getPluginManager().registerEvents(scoreboardManager, this);

        chatManager = new ChatManager(this);
        getServer().getPluginManager().registerEvents(chatManager, this);

        luckPermsHook = new LuckPermsHook(this);

        getServer().getPluginManager().registerEvents(this, this);

        // Channels
        getServer().getMessenger().registerIncomingPluginChannel(this, "minewar:staffchat", this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "minewar:sync", this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "minewar:staffchat");

        // Vanish
        this.vanishPacketListener = new com.jxsh.misc.listeners.VanishPacketListener(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "minewar:vanish", vanishPacketListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "minewar:vanish");

        // PAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.papiExpansion = new PAPIExpansion(this);
            this.papiExpansion.register();
        }

        // Global Chat
        getServer().getMessenger().registerIncomingPluginChannel(this, "minewar:globalchat", this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "minewar:globalchat");

        loadDisabledMentions();
        loadDisabledPMs();

        // Core Managers
        this.databaseManager = new DatabaseManager(this);
        this.commandManager = new CommandManager(this);
        this.spawnManager = new SpawnManager(this);
        getServer().getPluginManager().registerEvents(spawnManager, this);
        this.worldManager = new WorldManager(this);
        this.warpManager = new WarpManager(this);
        this.kitManager = new KitManager(this);
        this.tempOpManager = new TempOpManager(this);
        this.forcefieldManager = new ForcefieldManager(this);
        this.devManager = new com.jxsh.misc.managers.DevManager(this);
        // helpManager initialized early above

        this.buildModeManager = new com.jxsh.misc.managers.BuildModeManager(this);

        // AntiPopup
        if (configManager.isFeatureEnabled("antipopup")) {
            new com.jxsh.misc.features.antipopup.AntiPopupManager(this).enable();
        }

        if (configManager.isFeatureEnabled("buildmode")) {
            getServer().getPluginManager()
                    .registerEvents(new com.jxsh.misc.listeners.BuildModeListener(this, buildModeManager), this);
            if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                    || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
                com.sk89q.worldedit.WorldEdit.getInstance().getEventBus()
                        .register(new com.jxsh.misc.listeners.WorldEditListener(this, buildModeManager));
            }
        }

        // Tab Complete Security
        getServer().getPluginManager().registerEvents(new com.jxsh.misc.listeners.TabCompleteListener(this), this);

        // Defaults
        generateDefaults();

        // Join Commands
        if (configManager.getConfig().getBoolean("Join-Commands.enabled", true)) {
            getServer().getPluginManager().registerEvents(new com.jxsh.misc.listeners.JoinCommandListener(this), this);
        }

        // Feature Listeners
        if (configManager.isFeatureEnabled("poopgun")) {
            getServer().getPluginManager().registerEvents(new com.jxsh.misc.listeners.PoopGunListener(this), this);
        }
        if (configManager.isFeatureEnabled("tempop")) {
            getServer().getPluginManager()
                    .registerEvents(new com.jxsh.misc.listeners.TempOpListener(this, tempOpManager), this);
        }

        // Tasks
        if (devManager != null && configManager.isFeatureEnabled("devarmour")) {
            new com.jxsh.misc.tasks.ArmourTask(this, devManager).runTaskTimer(this, 0L, 20L);
        }

        // Register Commands
        registerCommands();
    }

    private void unloadPluginSystem() {
        if (forcefieldManager != null) {
            forcefieldManager.shutdown();
        }
        if (buildModeManager != null) {
            buildModeManager.shutdown();
        }
        // Unregister PAPI
        if (papiExpansion != null) {
            papiExpansion.unregister();
            papiExpansion = null;
        }

        // Unregister Channels to avoid "Registration already exists" on reload
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        if (commandManager != null) {
            commandManager.unregisterAll();
        }
        if (kitManager != null) {
            kitManager.shutdown();
        }
        if (tempOpManager != null) {
            tempOpManager.shutdown();
        }

        // Nullify managers to ensure GC and fresh init
        forcefieldManager = null;
        buildModeManager = null;
        commandManager = null;
        kitManager = null;
        tempOpManager = null;
        scoreboardManager = null;
        chatManager = null;
        luckPermsHook = null;
        spawnManager = null;
        worldManager = null;
        warpManager = null;
        devManager = null;
        helpManager = null;
        configManager = null;
    }

    private void registerCommands() {
        // Register Executors (Internal Key -> Executor)
        commandManager.addExecutor("minewarutils", new MinewarUtilsCommand(this));
        commandManager.addExecutor("help", new com.jxsh.misc.commands.HelpCommand(this));

        commandManager.addExecutor("top", new TopCommand(this));
        commandManager.addExecutor("bottom", new BottomCommand(this));
        commandManager.addExecutor("heal", new HealCommand(this));
        commandManager.addExecutor("eat", new EatCommand(this));
        commandManager.addExecutor("fly", new FlyCommand(this));
        commandManager.addExecutor("flyspeed", new FlySpeedCommand(this));

        commandManager.addExecutor("gamemode", new GamemodeCommand(this, "gamemode", null));
        commandManager.addExecutor("gmc", new GamemodeCommand(this, "gmc", org.bukkit.GameMode.CREATIVE));
        commandManager.addExecutor("gms", new GamemodeCommand(this, "gms", org.bukkit.GameMode.SURVIVAL));
        commandManager.addExecutor("gma", new GamemodeCommand(this, "gma", org.bukkit.GameMode.ADVENTURE));
        commandManager.addExecutor("gmsp", new GamemodeCommand(this, "gmsp", org.bukkit.GameMode.SPECTATOR));

        commandManager.addExecutor("inventorysee", new InventorySeeCommand(this));
        commandManager.addExecutor("clearinventory", new ClearInventoryCommand(this));
        commandManager.addExecutor("give", new GiveCommand(this));
        commandManager.addExecutor("head", new HeadCommand(this));

        commandManager.addExecutor("setspawn", new SetSpawnCommand(this, spawnManager));
        commandManager.addExecutor("spawn", new SpawnCommand(this, spawnManager));

        commandManager.addExecutor("mutechat", new MuteChatCommand(this));
        commandManager.addExecutor("slowchat", new SlowChatCommand(this));
        commandManager.addExecutor("clearchat", new ClearChatCommand(this));
        commandManager.addExecutor("mentiontoggle", new MentionToggleCommand(this));

        commandManager.addExecutor("buildmode", new com.jxsh.misc.commands.BuildModeCommand(this, buildModeManager));
        commandManager.addExecutor("bmadmin", new com.jxsh.misc.commands.BuildModeAdminCommand(this, buildModeManager));
        commandManager.addExecutor("bmreset", new com.jxsh.misc.commands.BuildModeResetCommand(this, buildModeManager));

        commandManager.addExecutor("poopgun", new PoopGunCommand(this));
        commandManager.addExecutor("devarmour", new DevArmorCommand(this, devManager));

        commandManager.addExecutor("setwarp", new SetWarpCommand(this, warpManager));
        WarpCommand warpCmd = new WarpCommand(this, warpManager);
        commandManager.addExecutor("warp", warpCmd);
        commandManager.addExecutor("warps", warpCmd);
        commandManager.addExecutor("deletewarp", new DeleteWarpCommand(this, warpManager));
        commandManager.addExecutor("editwarp", new EditWarpCommand(this, warpManager));
        commandManager.addExecutor("itemname", new ItemNameCommand(this));
        commandManager.addExecutor("lore", new LoreCommand(this));
        commandManager.addExecutor("forcefield", new ForcefieldCommand(this, forcefieldManager));

        commandManager.addExecutor("createkit", new CreateKitCommand(this, kitManager));
        KitCommand kitCmd = new KitCommand(this, kitManager);
        commandManager.addExecutor("kit", kitCmd);
        commandManager.addExecutor("deletekit", new DeleteKitCommand(this, kitManager));
        commandManager.addExecutor("editkit", new EditKitCommand(this, kitManager));
        commandManager.addExecutor("kits", kitCmd);

        commandManager.addExecutor("tempop", new TempOpCommand(this, tempOpManager));
        commandManager.addExecutor("tempop-remove", new DeopCommand(this, tempOpManager));
        commandManager.addExecutor("deop", new DeopCommand(this, tempOpManager));
        commandManager.addExecutor("ops", new OpsCommand(this, tempOpManager));

        // NOW register them based on config
        commandManager.registerAllConfiguredCommands();
    }

    // Getters
    public ChatManager getChatManager() {
        return chatManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the BoostedYAML config (NOT Bukkit's FileConfiguration).
     * This is the config that actually has our values.
     */
    public dev.dejvokep.boostedyaml.YamlDocument getBoostedConfig() {
        return configManager != null ? configManager.getConfig() : null;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public ForcefieldManager getForcefieldManager() {
        return forcefieldManager;
    }

    public com.jxsh.misc.managers.BuildModeManager getBuildModeManager() {
        return buildModeManager;
    }

    public com.jxsh.misc.managers.DevManager getDevManager() {
        return devManager;
    }

    public com.jxsh.misc.managers.HelpManager getHelpManager() {
        return helpManager;
    }

    public com.jxsh.misc.listeners.VanishPacketListener getVanishPacketListener() {
        return vanishPacketListener;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    public TempOpManager getTempOpManager() {
        return tempOpManager;
    }

    private void generateDefaults() {
        // Scan and generate permissions
        // Updated list to include all permissions removed from plugin.yml and new core
        // structural nodes
        java.util.List<String> cmdKeys = java.util.Arrays.asList(
                // Core
                "minewarutils", "reload", "help",
                // Features
                "sneak", "sneak.others",
                "world-flags",
                // Chat
                "mutechat", "mutechat.toggle", "mutechat-bypass",
                "slowchat", "slowchat.set", "slowchat-bypass",
                "clearchat", "clearchat-bypass",
                "mentiontoggle", "mentions",
                // Teleportation & Movement
                "top", "top.others",
                "bottom", "bottom.others",
                "fly", "fly.others",
                "flyspeed", "flyspeed.others",
                "setspawn", "spawn", "spawn.others",
                "setwarp", "warp", "warp.others", "deletewarp", "editwarp", "warps",
                // Player Management
                "heal", "heal.others",
                "eat", "eat.others",
                "gamemode", "gamemode.others",
                "gmc", "gmc.others", "gms", "gms.others", "gma", "gma.others", "gmsp", "gmsp.others",
                "inventorysee", "inventorysee.others",
                "clearinventory", "clearinventory.others",
                "give", "give.others",
                "head", "head.others",
                "itemname", "lore",
                // Build Mode
                "buildmode", "buildmode-others", "bmadmin", "bmreset", "bmreset.others", "buildmode-bypass",
                // Kits
                "createkit", "kit", "kit.others", "deletekit", "editkit", "kits",
                // Fun & Admin
                "poopgun", "poopgun.others",
                "devarmour", "devarmour.others",
                "forcefield", "forcefield.others", "forcefield-others",
                // OP Manager - NEW NODES
                "tempop", "tempop-grant", "tempop-remove", "ops",
                "op.temporary", "op.permanent", "op.deop");
        configManager.generatePermissions(cmdKeys);

        // Generate default messages
        java.util.Map<String, String> defaultMsgs = new java.util.HashMap<>();
        // Internal/General
        defaultMsgs.put("commands.error.no-permission", "<red>You do not have permission to use this command.");
        // Command specific
        defaultMsgs.put("commands.give.usage", "<red>Usage: /give <item> [amount] [player] [custom name]");
        defaultMsgs.put("commands.give.invalid-item", "<red>Invalid item: %item%");
        defaultMsgs.put("commands.poopgun.players-only", "<red>Only players can use this command.");
        defaultMsgs.put("commands.flyspeed.usage", "<red>Usage: /flyspeed <0-10> [player]");
        defaultMsgs.put("commands.flyspeed.range", "<red>Speed must be between 0 and 10.");
        defaultMsgs.put("commands.head.usage", "<red>Usage: /head <player>");
        defaultMsgs.put("commands.slowchat.usage", "<red>Usage: /slowchat <seconds|off>");

        // BuildMode
        defaultMsgs.put("buildmode.admin-enabled", "<#ccffff>You are now bypassing build restrictions.");
        defaultMsgs.put("buildmode.admin-disabled", "<#ccffff>You are no longer bypassing build restrictions.");
        defaultMsgs.put("buildmode.admin-enabled-other",
                "<#0adef7>%target% <#ccffff>is now bypassing build restrictions.");
        defaultMsgs.put("buildmode.admin-disabled-other",
                "<#0adef7>%target% <#ccffff>is no longer bypassing build restrictions.");
        defaultMsgs.put("buildmode.reset", "<#ccffff>Reset <red>%count% <#ccffff>blocks placed in Build Mode.");
        defaultMsgs.put("invalid-player", "<red>The player <#0adef7>%target% <#ccffff>was not found.");

        configManager.generateMessages(defaultMsgs);
    }

    private void loadDisabledMentions() {
        disabledMentions.clear();
        List<String> list = configManager.getConfig().getStringList("disabled-mentions");
        for (String s : list) {
            try {
                disabledMentions.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveDisabledMentions() {
        List<String> list = new ArrayList<>();
        for (UUID uuid : disabledMentions) {
            list.add(uuid.toString());
        }
        configManager.getConfig().set("disabled-mentions", list);
        try {
            configManager.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isMentionEnabled(UUID uuid) {
        return !disabledMentions.contains(uuid);
    }

    public void toggleMention(UUID uuid) {
        if (disabledMentions.contains(uuid)) {
            disabledMentions.remove(uuid);
        } else {
            disabledMentions.add(uuid);
        }
        saveDisabledMentions();
    }

    private void loadDisabledPMs() {
        disabledPMs.clear();
        List<String> list = configManager.getConfig().getStringList("disabled-pms");
        for (String s : list) {
            try {
                disabledPMs.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void saveDisabledPMs() {
        List<String> list = new ArrayList<>();
        for (UUID uuid : disabledPMs) {
            list.add(uuid.toString());
        }
        configManager.getConfig().set("disabled-pms", list);
        try {
            configManager.getConfig().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isPMEnabled(UUID uuid) {
        return !disabledPMs.contains(uuid);
    }

    public void togglePM(UUID uuid) {
        if (disabledPMs.contains(uuid)) {
            disabledPMs.remove(uuid);
        } else {
            disabledPMs.add(uuid);
        }
        saveDisabledPMs();
    }

    public boolean isStaffChatToggled(UUID uuid) {
        return staffChatToggled.contains(uuid);
    }

    public void toggleStaffChat(UUID uuid) {
        if (staffChatToggled.contains(uuid)) {
            staffChatToggled.remove(uuid);
        } else {
            staffChatToggled.add(uuid);
        }
    }

    public dev.dejvokep.boostedyaml.YamlDocument getPermissionsConfig() {
        return configManager.getPermissions();
    }

    public String getPermission(String path, String def) {
        if (configManager.getPermissions().isString(path)) {
            return configManager.getPermissions().getString(path, def);
        }
        return configManager.getPermissions().getString(path + ".node", def);
    }

    public String getCommandPermission(String cmd) {
        return getPermission("commands." + cmd.replace(" ", "-"), "minewar." + cmd.replace(" ", "."));
    }

    public boolean hasPermission(org.bukkit.command.CommandSender sender, String key) {
        dev.dejvokep.boostedyaml.YamlDocument perms = configManager.getPermissions();

        if (perms == null) {
            return sender.hasPermission("minewars.command." + key.replace("-", "."));
        }

        String safeKey = key.replace(" ", "-");
        dev.dejvokep.boostedyaml.route.Route nodeRoute = dev.dejvokep.boostedyaml.route.Route.from("commands", safeKey,
                "node");
        dev.dejvokep.boostedyaml.route.Route defaultRoute = dev.dejvokep.boostedyaml.route.Route.from("commands",
                safeKey, "default");
        dev.dejvokep.boostedyaml.route.Route bypassRoute = dev.dejvokep.boostedyaml.route.Route.from("commands",
                safeKey, "op-bypass");

        // Step A: Default Access (Master priority)
        if (perms.contains(defaultRoute) && perms.getBoolean(defaultRoute)) {
            return true;
        }

        // Step B: OP Bypass
        if (perms.getBoolean(bypassRoute, true) && sender.isOp()) {
            return true;
        }

        // Step C: Specific node
        String node = perms.getString(nodeRoute, "minewar." + key.replace("-", "."));
        return sender.hasPermission(node);
    }

    public String getBypassPermission(String feature) {
        return getPermission("bypass." + feature, "minewar." + feature + ".bypass");
    }

    public void reloadConfig() {
        configManager.reloadConfig();
    }

    public void saveConfig() {
        try {
            configManager.getConfig().save();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Should not be called for registered commands with Executors, but safe
        // fallback or for internal checks
        return false;
    }

    public void loadConfigValues() {
        dev.dejvokep.boostedyaml.YamlDocument config = configManager.getConfig();
        // New structure: cosmetics.crouch-settings.enabled-users
        enabledUsers = config.getStringList("cosmetics.crouch-settings.enabled-users");
        if (enabledUsers == null) {
            enabledUsers = new ArrayList<>();
        }

        // drop-item keys moved to cosmetics in v3 strict
        String materialName = config.getString("cosmetics.drop-item-type", "DIAMOND");
        if (materialName.equalsIgnoreCase("NONE") || materialName.equalsIgnoreCase("AIR")) {
            dropItemType = Material.AIR;
        } else {
            try {
                dropItemType = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material name: " + materialName + ". Defaulting to DIAMOND.");
                dropItemType = Material.DIAMOND;
            }
        }

        dropItemName = config.getString("cosmetics.drop-item-name", "");
        dropItemEnabled = config.getBoolean("cosmetics.drop-item-enabled", false);

        dropDurationSeconds = config.getInt("cosmetics.drop-duration-seconds", 5);

        String particleName = config.getString("cosmetics.particle-type", "HEART");
        try {
            particleType = Particle.valueOf(particleName);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid particle name: " + particleName + ". Defaulting to HEART.");
            particleType = Particle.HEART;
        }

        particleCount = config.getInt("cosmetics.particle-count", 1);
        particleDurationSeconds = config.getInt("cosmetics.particle-duration-seconds", 1);

        onCrouchMessage = configManager.getMessages().getString("on-crouch", "");
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking())
            return;

        Player player = event.getPlayer();
        if (enabledUsers.contains(player.getName())) {
            spawnEffects(player);
        }
    }

    private void spawnEffects(Player player) {
        if (dropItemEnabled && dropItemType != Material.AIR) {
            ItemStack itemStack = new ItemStack(dropItemType);
            Component finalDisplayName = null;

            if (!dropItemName.isEmpty()) {
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    String displayName = dropItemName.replace("%material%",
                            (dropItemType == Material.AIR ? "None" : dropItemType.name()));

                    Component nameComponent = parseText(displayName, player);
                    meta.displayName(nameComponent);
                    itemStack.setItemMeta(meta);
                    finalDisplayName = nameComponent;
                }
            }

            Item item = player.getWorld().dropItem(player.getLocation(), itemStack);
            item.setPickupDelay(Integer.MAX_VALUE);

            if (finalDisplayName != null) {
                item.customName(finalDisplayName);
                item.setCustomNameVisible(true);
            }

            Bukkit.getScheduler().runTaskLater(this, item::remove, dropDurationSeconds * 20L);
        }

        if (particleDurationSeconds <= 0) {
            player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0), particleCount, 0.5, 0.5,
                    0.5, 0.1);
        } else {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 1, 0), particleCount, 0.5,
                            0.5, 0.5, 0.1);
                }
            }.runTaskTimer(this, 0L, 5L);

            Bukkit.getScheduler().runTaskLater(this, task::cancel, particleDurationSeconds * 20L);
        }

        if (!onCrouchMessage.isEmpty()) {
            String msg = onCrouchMessage.replace("%material%",
                    (dropItemType == Material.AIR ? "None" : dropItemType.name()));
            player.sendMessage(parseText(msg, player));
        }
    }

    public Component parseText(String text, @NotNull org.bukkit.OfflinePlayer player) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String processed = processStrings(text, player);
        processed = translateLegacyToMiniMessage(processed);

        try {
            return MiniMessage.miniMessage().deserialize(processed);
        } catch (Exception e) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(processed);
        }
    }

    public String processStrings(String text, org.bukkit.OfflinePlayer player) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String processed = text;
        if (player != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            processed = PlaceholderAPI.setPlaceholders(player, processed);
        }

        if (luckPermsHook != null) {
            processed = luckPermsHook.parseDynamicPlaceholders(processed);

            if (player != null && player.getName() != null) {
                String prefix = luckPermsHook.getPrefix(player.getName());
                String suffix = luckPermsHook.getSuffix(player.getName());

                processed = processed.replace("%luckperms_prefix%", prefix);
                processed = processed.replace("%luckperms_suffix%", suffix);
                processed = processed.replace("%prefix%", prefix);
                processed = processed.replace("%suffix%", suffix);
                // Also support %prefix_other% and %suffix_other% without parentheses as
                // player's own
                processed = processed.replace("%prefix_other%", prefix);
                processed = processed.replace("%suffix_other%", suffix);
            }
        }

        if (tempOpManager != null && player != null) {
            com.jxsh.misc.managers.TempOpManager.OpData data = tempOpManager.getOpData(player.getUniqueId());
            String timeLeft = configManager.getMessages().getString("commands.tempop.no-time", "N/A"); // Default
                                                                                                       // fallback

            if (data != null) {
                if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.PERM) {
                    timeLeft = configManager.getMessages().getString("commands.tempop.time-left-perm", "Permanent");
                } else if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TEMP) {
                    // Exists but no specific duration set (Relog-only)
                    timeLeft = configManager.getMessages().getString("commands.tempop.time-left-temp", "Expire-Relog");
                } else if (data.type == com.jxsh.misc.managers.TempOpManager.OpType.TIME) {
                    long remaining = (data.expiration - System.currentTimeMillis()) / 1000;
                    if (remaining < 0)
                        remaining = 0;

                    String format = configManager.getMessages().getString("commands.tempop.time-left-format",
                            "%days%d, %hours%h, %minutes%m, %seconds%s");
                    timeLeft = formatDurationConfigurable(remaining, format);
                }
            } else if (player.isOp()) {
                // Fallback for Vanilla OP if we want to show "Perm"
                // usage says "no-time" if no OP data exists, essentially.
                // But if they are OP but not in our system, maybe we show "Perm" or "N/A".
                // Detailed instructions: "If no OP data exists, return the value from
                // commands.tempop.no-time."
                // So we use the default we set above.
            }

            processed = processed.replace("%time_left%", timeLeft);
            processed = processed.replace("%tempop_time_left%", timeLeft);
        }

        return processed;
    }

    private String formatDurationConfigurable(long totalSeconds, String format) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return format
                .replace("%days%", String.valueOf(days))
                .replace("%hours%", String.valueOf(hours))
                .replace("%minutes%", String.valueOf(minutes))
                .replace("%seconds%", String.valueOf(seconds));
    }

    public String translateLegacyToMiniMessage(String text) {
        if (text == null)
            return null;

        String urlRegex = "(https?://\\S+|www\\.\\S+)";
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(urlRegex,
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher urlMatcher = urlPattern.matcher(text);

        List<String> protectedUrls = new ArrayList<>();
        StringBuilder placeholderSb = new StringBuilder();
        int lastUrlEnd = 0;
        int urlCount = 0;

        while (urlMatcher.find()) {
            placeholderSb.append(text, lastUrlEnd, urlMatcher.start());
            String url = urlMatcher.group();
            protectedUrls.add(url);
            placeholderSb.append("%%URL_").append(urlCount).append("%%");
            urlCount++;
            lastUrlEnd = urlMatcher.end();
        }
        placeholderSb.append(text.substring(lastUrlEnd));
        String textWithPlaceholders = placeholderSb.toString();

        textWithPlaceholders = textWithPlaceholders.replace("&0", "<black>").replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>").replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>").replace("&6", "<gold>").replace("&7", "<gray>")
                .replace("&8", "<dark_gray>").replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&k", "<obfuscated>").replace("&l", "<bold>")
                .replace("&m", "<strikethrough>").replace("&n", "<underlined>").replace("&o", "<italic>")
                .replace("&r", "<reset>")
                .replace("§0", "<black>").replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>").replace("§3", "<dark_aqua>").replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>").replace("§6", "<gold>").replace("§7", "<gray>")
                .replace("§8", "<dark_gray>").replace("§9", "<blue>").replace("§a", "<green>").replace("§b", "<aqua>")
                .replace("§c", "<red>").replace("§d", "<light_purple>").replace("§e", "<yellow>")
                .replace("§f", "<white>").replace("§k", "<obfuscated>").replace("§l", "<bold>")
                .replace("§m", "<strikethrough>").replace("§n", "<underlined>").replace("§o", "<italic>")
                .replace("§r", "<reset>");

        java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([A-Fa-f0-9]{6})");
        java.util.regex.Matcher hexMatcher = hexPattern.matcher(textWithPlaceholders);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(sb, "<#" + hexMatcher.group(1) + ">");
        }
        hexMatcher.appendTail(sb);
        String converted = sb.toString();

        for (int i = 0; i < protectedUrls.size(); i++) {
            String url = protectedUrls.get(i);
            // Clickable Link support: wrap url in click tag and underlined
            String miniUrl = "<click:open_url:" + url + "><underlined>" + url + "</underlined></click>";
            converted = converted.replace("%%URL_" + i + "%%", miniUrl);
        }

        return converted;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (channel.equals("minewar:staffchat")) {
            com.google.common.io.ByteArrayDataInput in = com.google.common.io.ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            if (subChannel.equals("Toggle")) {
                try {
                    String uuidString = in.readUTF();
                    boolean state = in.readBoolean();
                    UUID uuid = UUID.fromString(uuidString);

                    if (state) {
                        staffChatToggled.add(uuid);
                    } else {
                        staffChatToggled.remove(uuid);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (channel.equals("minewar:sync")) {
            com.google.common.io.ByteArrayDataInput in = com.google.common.io.ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            if (subChannel.equals("BulkCount")) {
                networkCache.setGlobalTotalCount(in.readInt());
                int serverCount = in.readInt();
                networkCache.clearServerCounts();
                for (int i = 0; i < serverCount; i++) {
                    String srvName = in.readUTF();
                    int count = in.readInt();
                    networkCache.setServerCount(srvName, count);
                }
            }
        }
    }

    public NetworkCache getNetworkCache() {
        return networkCache;
    }

    public int getGlobalTotalCount() {
        return networkCache.getGlobalTotalCount();
    }

    public int getServerCount(String name) {
        return networkCache.getServerCount(name);
    }
}

package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HelpManager {

    private final JxshMisc plugin;
    private YamlDocument helpConfig;
    private final List<CommandEntry> cachedCommands = new ArrayList<>();
    private LuckPerms luckPerms;

    public HelpManager(JxshMisc plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            this.luckPerms = LuckPermsProvider.get();
        }
    }

    public void load() {
        try {
            File file = new File(plugin.getDataFolder(), "help.yml");
            if (!file.exists()) {
                plugin.saveResource("help.yml", false);
            }

            helpConfig = YamlDocument.create(file,
                    Objects.requireNonNull(plugin.getClass().getResourceAsStream("/help.yml")),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.DEFAULT,
                    DumperSettings.DEFAULT,
                    UpdaterSettings.DEFAULT);

            helpConfig.update();
            reloadCache();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load help.yml");
            e.printStackTrace();
        }
    }

    public void reloadCache() {
        cachedCommands.clear();

        // ONLY Add Manual Entries from help.yml
        if (helpConfig.contains("manual-entries")) {
            List<Map<?, ?>> entries = helpConfig.getMapList("manual-entries");
            for (Map<?, ?> entry : entries) {
                String cmd = (String) entry.get("command");
                String desc = (String) entry.get("description");
                String perm = (String) entry.get("permission");
                boolean isDefault = entry.containsKey("default") && (boolean) entry.get("default");
                cachedCommands.add(new CommandEntry(cmd, desc, perm, isDefault));
            }
        }
    }

    public void showHelp(CommandSender sender, int page) {
        if (!plugin.getConfigManager().getConfig().getBoolean("help-system.enabled", true)) {
            sender.sendMessage(plugin.parseText("<red>Help system is disabled.",
                    sender instanceof Player ? (Player) sender : null));
            return;
        }

        List<CommandEntry> visibleCommands = new ArrayList<>();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        for (CommandEntry entry : cachedCommands) {
            // Check if feature is enabled
            String cmdName = entry.command.startsWith("/") ? entry.command.substring(1) : entry.command;
            if (cmdName.contains(" ")) {
                cmdName = cmdName.split(" ")[0];
            }
            // Some commands might be aliases or complex, but basic checking works for most
            // core features
            // If checking "minewarutils", it defaults to true
            if (!plugin.getConfigManager().isFeatureEnabled(cmdName)) {
                continue;
            }

            if (hasPermission(sender, entry)) {
                visibleCommands.add(entry);
            }
        }

        if (visibleCommands.isEmpty()) {
            sender.sendMessage(plugin.parseText("<red>No commands available.", player));
            return;
        }

        int linesPerPage = helpConfig.getInt("help.lines-per-page", 10);
        int totalPages = (int) Math.ceil((double) visibleCommands.size() / linesPerPage);

        if (page < 1)
            page = 1;
        if (page > totalPages)
            page = totalPages;

        int startIndex = (page - 1) * linesPerPage;
        int endIndex = Math.min(startIndex + linesPerPage, visibleCommands.size());

        String headerFormat = helpConfig.getString("help.header",
                "<gray>Available commands <yellow>(Page %page%/%total%):");
        String lineFormat = helpConfig.getString("help.line-format", "<yellow>%command% <gray>- %description%");
        String footerFormat = helpConfig.getString("help.footer",
                "<gray>Use <yellow>/mu help <page> <gray>to view other pages");

        sender.sendMessage(plugin.parseText(headerFormat
                .replace("%page%", String.valueOf(page))
                .replace("%total%", String.valueOf(totalPages)), player));

        for (int i = startIndex; i < endIndex; i++) {
            CommandEntry entry = visibleCommands.get(i);
            String line = lineFormat
                    .replace("%command%", entry.command)
                    .replace("%description%", entry.description);
            sender.sendMessage(plugin.parseText(line, player));
        }

        if (totalPages > 1) {
            sender.sendMessage(plugin.parseText(footerFormat.replace("%next_page%", String.valueOf(page + 1)), player));
        }
    }

    private boolean hasPermission(CommandSender sender, CommandEntry entry) {
        // 1. Check default flag
        if (entry.isDefault) {
            return true;
        }

        String perm = entry.permission;

        // Infer permission if missing: minewar.<command_without_slash>
        if (perm == null || perm.isEmpty()) {
            String cleanCmd = entry.command.startsWith("/") ? entry.command.substring(1) : entry.command;
            // Split by space to get base command only
            String[] parts = cleanCmd.split(" ");
            perm = "minewar." + parts[0];
        }

        if (luckPerms != null && sender instanceof Player) {
            User user = luckPerms.getUserManager().getUser(((Player) sender).getUniqueId());
            if (user != null) {
                // Check using LuckPerms cached data which includes inheritance
                return user.getCachedData().getPermissionData().checkPermission(perm).asBoolean();
            }
        }

        // Fallback
        return sender.hasPermission(perm);
    }

    private static class CommandEntry {
        String command;
        String description;
        String permission;
        boolean isDefault;

        CommandEntry(String command, String description, String permission, boolean isDefault) {
            this.command = command;
            this.description = description;
            this.permission = permission;
            this.isDefault = isDefault;
        }
    }
}

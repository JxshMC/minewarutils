package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class WarpManager {

    private final JxshMisc plugin;
    private YamlDocument warpsConfig;

    public WarpManager(JxshMisc plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        try {
            // Create warps.yml if it doesn't exist. We don't need a default resource for
            // this one necessarily,
            // but BoostedYAML likes one. If we don't have one, we can just create a file.
            File databaseFolder = new File(plugin.getDataFolder(), "Database");
            if (!databaseFolder.exists()) {
                databaseFolder.mkdirs();
            }
            File file = new File(databaseFolder, "warps.yml");

            if (plugin.getResource("warps.yml") != null) {
                warpsConfig = YamlDocument.create(
                        file,
                        plugin.getClass().getResourceAsStream("/warps.yml"),
                        GeneralSettings.builder().setUseDefaults(false).build(),
                        LoaderSettings.builder().setAutoUpdate(true).build(),
                        DumperSettings.DEFAULT,
                        UpdaterSettings.builder().setKeepAll(true).build());
            } else {
                warpsConfig = YamlDocument.create(
                        file,
                        GeneralSettings.builder().setUseDefaults(false).build(),
                        LoaderSettings.builder().setAutoUpdate(true).build(),
                        DumperSettings.DEFAULT,
                        UpdaterSettings.builder().setKeepAll(true).build());
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load warps.yml!");
            e.printStackTrace();
        }
    }

    public void setWarp(String name, Location loc) {
        if (warpsConfig == null)
            return;

        String path = "warps." + name.toLowerCase();
        warpsConfig.set(path + ".world", loc.getWorld().getName());
        warpsConfig.set(path + ".x", loc.getX());
        warpsConfig.set(path + ".y", loc.getY());
        warpsConfig.set(path + ".z", loc.getZ());
        warpsConfig.set(path + ".yaw", loc.getYaw());
        warpsConfig.set(path + ".pitch", loc.getPitch());

        save();
    }

    public Location getWarp(String name) {
        if (warpsConfig == null)
            return null;

        String path = "warps." + name.toLowerCase();
        if (!warpsConfig.contains(path))
            return null;

        String worldName = warpsConfig.getString(path + ".world");
        double x = warpsConfig.getDouble(path + ".x");
        double y = warpsConfig.getDouble(path + ".y");
        double z = warpsConfig.getDouble(path + ".z");
        float yaw = warpsConfig.getFloat(path + ".yaw");
        float pitch = warpsConfig.getFloat(path + ".pitch");

        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;

        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean deleteWarp(String name) {
        if (warpsConfig == null)
            return false;
        String path = "warps." + name.toLowerCase();
        if (warpsConfig.contains(path)) {
            warpsConfig.remove(path);
            save();
            return true;
        }
        return false;
    }

    public Set<String> getWarps() {
        if (warpsConfig == null || !warpsConfig.contains("warps"))
            return Collections.emptySet();
        return warpsConfig.getSection("warps").getRoutesAsStrings(false);
    }

    public void save() {
        try {
            warpsConfig.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        try {
            warpsConfig.reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerMoveEvent;

public class WorldManager implements Listener {

    private final JxshMisc plugin;

    public WorldManager(JxshMisc plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Apply initial settings for existing worlds
        for (World world : Bukkit.getWorlds()) {
            applyWorldSettings(world);
        }
    }

    private void applyWorldSettings(World world) {
        if (checkSetting(world, "0tick", false)) {
            world.setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0);
        }
    }

    @EventHandler
    public void onWorldLoad(org.bukkit.event.world.WorldLoadEvent event) {
        applyWorldSettings(event.getWorld());
    }

    public void setWorldSetting(World world, String setting, String value, Player sender) {
        String path = "worlds." + world.getName() + "." + setting.toLowerCase();

        switch (setting.toLowerCase()) {
            case "destroy":
            case "place":
            case "pvp":
            case "invincible":
            case "spawnmobs":
                boolean boolVal = Boolean.parseBoolean(value);
                plugin.getConfigManager().getConfig().set(path, boolVal);
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("world-setting-set")
                                .replace("%setting%", setting)
                                .replace("%value%", String.valueOf(boolVal))
                                .replace("%world%", world.getName()), sender));

                if (setting.equalsIgnoreCase("invincible") && boolVal) {
                    // Heal all players in world immediately
                    for (Player p : world.getPlayers()) {
                        p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                        p.setFoodLevel(20);
                    }
                }
                break;

            case "nomobs":
                boolean noMobs = Boolean.parseBoolean(value);
                // Depency check: SpawnMobs must be false
                boolean spawnMobs = plugin.getConfigManager().getConfig()
                        .getBoolean("worlds." + world.getName() + ".spawnmobs", true);
                if (noMobs && spawnMobs) {
                    sender.sendMessage(plugin
                            .parseText(plugin.getConfigManager().getMessages().getString("world-nomobs-fail"), sender));
                    return;
                }

                plugin.getConfigManager().getConfig().set(path, noMobs);
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("world-setting-set")
                                .replace("%setting%", "NoMobs")
                                .replace("%value%", String.valueOf(noMobs))
                                .replace("%world%", world.getName()), sender));

                if (noMobs) {
                    butcherWorld(world);
                }
                break;

            case "antivoid":
                boolean antiVoid = Boolean.parseBoolean(value);
                plugin.getConfigManager().getConfig().set(path, antiVoid);
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("world-setting-set")
                                .replace("%setting%", "AntiVoid")
                                .replace("%value%", String.valueOf(antiVoid))
                                .replace("%world%", world.getName()), sender));
                break;

            case "antivoidlevel":
                try {
                    int level = Integer.parseInt(value);
                    plugin.getConfigManager().getConfig().set("worlds." + world.getName() + ".antivoid-level", level);
                    sender.sendMessage(
                            plugin.parseText(plugin.getConfigManager().getMessages().getString("world-setting-set")
                                    .replace("%setting%", "AntiVoid Level")
                                    .replace("%value%", String.valueOf(level))
                                    .replace("%world%", world.getName()), sender));
                } catch (NumberFormatException e) {
                    sender.sendMessage(
                            plugin.parseText(plugin.getConfigManager().getMessages().getString("world-invalid-level")
                                    .replace("%value%", value), sender));
                }
                break;

            case "nodecay":
            case "blockdecay":
                boolean blockDecay = Boolean.parseBoolean(value);
                plugin.getConfigManager().getConfig().set("worlds." + world.getName() + ".blockdecay", blockDecay);
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("world-setting-set")
                                .replace("%setting%", "BlockDecay")
                                .replace("%value%", String.valueOf(blockDecay))
                                .replace("%world%", world.getName()), sender));
                break;

            case "0tick":
                boolean zeroTick = Boolean.parseBoolean(value);
                plugin.getConfigManager().getConfig().set(path, zeroTick);
                world.setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, zeroTick ? 0 : 3);
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("world-setting-set")
                                .replace("%setting%", "0Tick")
                                .replace("%value%", String.valueOf(zeroTick))
                                .replace("%world%", world.getName()), sender));
                break;

            default:
                sender.sendMessage(
                        plugin.parseText(plugin.getConfigManager().getMessages().getString("world-unknown-setting")
                                .replace("%setting%", setting), sender));
                return;
        }

        try {
            plugin.getConfigManager().getConfig().save();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void butcherWorld(World world) {
        int count = 0;
        for (Entity e : world.getEntities()) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                e.remove();
                count++;
            }
        }
        // Log to console?
    }

    // Listeners

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (!checkSetting(event.getPlayer().getWorld(), "destroy", true)) {
            if (!event.getPlayer().hasPermission("minewar.bypass.world")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (!checkSetting(event.getPlayer().getWorld(), "place", true)) {
            if (!event.getPlayer().hasPermission("minewar.bypass.world")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (!checkSetting(event.getEntity().getWorld(), "pvp", true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (event.getEntity() instanceof Player) {
            if (checkSetting(event.getEntity().getWorld(), "invincible", false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (event.getEntity() instanceof Player) {
            if (checkSetting(event.getEntity().getWorld(), "invincible", false)) {
                event.setCancelled(true);
                event.setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        World world = event.getLocation().getWorld();

        // NoMobs Check (Highest Priority - if true, cancel ALL)
        if (checkSetting(world, "nomobs", false)) {
            event.setCancelled(true);
            return;
        }

        // SpawnMobs Check (Only controls NATURAL)
        if (event.getSpawnReason() == SpawnReason.NATURAL) {
            if (!checkSetting(world, "spawnmobs", true)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        Player player = event.getPlayer();
        World world = player.getWorld();

        // Check if AntiVoid is enabled for this world or globally
        boolean enabled = checkSetting(world, "antivoid",
                plugin.getConfigManager().getConfig().getBoolean("AntiVoid.Enabled", true));

        if (!enabled)
            return;

        int minLevel = plugin.getConfigManager().getConfig().getInt("worlds." + world.getName() + ".antivoid-level",
                plugin.getConfigManager().getConfig().getInt("world-flags.antivoid-level",
                        plugin.getBoostedConfig().getInt("AntiVoid.Level", -64)));

        if (player.getLocation().getY() < minLevel) {
            plugin.getSpawnManager().teleportToSpawn(player);
        }
    }

    @EventHandler
    public void onLeafDecay(org.bukkit.event.block.LeavesDecayEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (checkSetting(event.getBlock().getWorld(), "blockdecay", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(org.bukkit.event.block.BlockFadeEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (checkSetting(event.getBlock().getWorld(), "blockdecay", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (checkSetting(event.getBlock().getWorld(), "blockdecay", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (checkSetting(event.getBlock().getWorld(), "blockdecay", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockForm(org.bukkit.event.block.BlockFormEvent event) {
        if (!plugin.getConfigManager().isFeatureEnabled("world-flags"))
            return;
        if (checkSetting(event.getBlock().getWorld(), "blockdecay", false)) {
            event.setCancelled(true);
        }
    }

    private boolean checkSetting(World world, String setting, boolean def) {
        // 1. Check world override
        if (plugin.getConfigManager().getConfig().contains("worlds." + world.getName() + "." + setting)) {
            return plugin.getConfigManager().getConfig().getBoolean("worlds." + world.getName() + "." + setting);
        }
        // 2. Check global default
        return plugin.getConfigManager().getConfig().getBoolean("world-flags." + setting, def);
    }
}

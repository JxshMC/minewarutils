# Minewar-Utils Plugin Instructions

## Overview
**Minewar-Utils** is a core utility plugin developed by **Jxsh** for the Minewar server network. It provides essential commands, gamemode management, item editing, warps, kits, and chat management tools.

**Version:** 1.0
**Authors:** Jxsh

## Configuration Management
The plugin uses **BoostedYAML** for robust configuration handling.
- **Config Files:** `config.yml`, `messages.yml`, `permissions.yml`, `scoreboard.yml` (if applicable), and database files in `Database/`.
- **Auto-Update:** Configuration files are automatically updated on server start/reload. New keys are added, and obsolete keys are removed (except in `config.yml` where user settings are preserved unless syntax is broken).
- **Validation:** Critical config values (booleans, numbers) are validated on load. Invalid entries are reset to defaults.
- **Formatting:** All customizable values are strictly double-quoted in YAML output.

## Command System
All commands are registered via `plugin.yml` and `JxshMisc.java`.
- **Base Command:** `/minewarutils` (Alias: `/mu`)
- **Help Command:** `/mu help` (Reads from `help.yml`)
- **Reload:** `/mu reload` (Reloads all configs)

### Core Commands
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/gamemode <mode> [player]` | `minewar.gamemode` | Toggle Creative/Survival or set specific mode. |
| `/gmc`, `/gms`, `/gma`, `/gmsp` | `minewar.gamemode.<mode>` | Shortcut for gamemodes. |
| `/fly [player]` | `minewar.fly` | Toggle flight. |
| `/flyspeed <0-10> [player]` | `minewar.flyspeed` | Set flight speed. |
| `/heal [player]` | `minewar.heal` | Heal a player. |
| `/eat [player]` | `minewar.eat` | Feed a player. |
| `/top`, `/bottom` | `minewar.top`, `minewar.bottom` | Teleport to highest/lowest block. |
| `/spawn [player]` | `minewar.spawn` | Teleport to spawn. |
| `/setspawn` | `minewar.setspawn` | Set the server spawn point. |

### Inventory & Items
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/give <item> [amount]` | `minewar.give` | Give items. |
| `/clearinventory [player]` | `minewar.clearinventory` | Clear inventory. |
| `/inventorysee <player>` | `minewar.inventorysee` | View/Edit allowed player inventory. |
| `/itemname <name>` | `minewar.itemname` | Rename item in hand (Supports MiniMessage). |
| `/lore <add/set/clear>` | `minewar.lore` | Manage item lore. |
| `/head <player>` | `minewar.head` | Get a player's head. |

### Warps & Kits
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/warp <name>` | `minewar.warp` | Teleport to a warp. |
| `/setwarp <name>` | `minewar.setwarp` | Create a warp. |
| `/delwarp <name>` | `minewar.deletewarp` | Delete a warp. |
| `/warps` | `minewar.warps` | List all warps. |
| `/kit <name>` | `minewar.kit` | Receive a kit. |
| `/createkit <name>` | `minewar.createkit` | Create a kit from inventory. |
| `/deletekit <name>` | `minewar.deletekit` | Delete a kit. |
| `/kits` | `minewar.kits` | List all kits. |

### Chat Management
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/mutechat` | `minewar.mutechat` | Mute global chat. |
| `/slowchat <seconds>` | `minewar.slowchat` | Set chat cooldown. |
| `/clearchat` | `minewar.clearchat` | Clear chat history. |
| `/mentiontoggle` | `minewar.mentiontoggle` | Toggle personal mentions. |

### Player Management & Misc
| Command | Permission | Description |
| :--- | :--- | :--- |
| `/tempop <grant/remove>` | `minewar.tempop` | Temporarily OP a player. |
| `/ops` | `minewar.ops` | List OPs. |
| `/poopgun` | `minewar.poopgun` | Fun cosmetic gun. |
| `/devarmour` | `minewar.devarmour` | Developer armor toggles. |
| `/buildmode [player]` | `minewar.buildmode` | Toggle restricted build mode. |
| `/forcefield` | `minewar.forcefield` | Admin forcefield. |

## Permissions
The permission system is strictly structured:
- **Root:** `minewar.`
- **Command:** `minewar.<command>`
- **Others:** `minewar.<command>.others` (Execute on other players)
- **Bypass:** `minewar.<command>.bypass` (Bypass restrictions, e.g., cooldowns)
- **Sub-commands:** `minewar.<command>.<subcommand>`

**Important:**
- `permissions.yml` is automatically generated with all available permissions.
- Default permissions are set to `false` (OPs only by default for most admin commands).

## Colors & Formatting
- **Primary Color:** `<#ccffff>`
- **Highlight/Secondary:** `<#0adef7>`
- **Error:** `<red>`
- **Target Names:** Always use `%suffix_other%` before the player name to include LuckPerms suffixes (e.g., `%suffix_other%%target%`).
- **MiniMessage:** All messages support MiniMessage formatting.

## Developer Notes
- **STRICT RULE:** No hardcoded messages, permission strings, or formats in Java files. Everything must be fetched from YAML files.
- **Code Style:** Always use `configManager.getMessages().getString()`.
- **Config Safety:** Use `ConfigManager.validateConfig()` to ensure critical values (spawn, worlds) are valid.
- **Help System:** `help.yml` controls the order and visibility of commands in `/mu help` and `/help`.

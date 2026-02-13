[COMPLETE-WIKI-GIST.md](https://github.com/user-attachments/files/25288340/COMPLETE-WIKI-GIST.md)
# Minewar-Utils Wiki - Complete Documentation

> **Note**: This is a consolidated version of all wiki pages for easy GitHub Gist creation.
> Each section below represents a separate wiki page.

---

# 📄 Home.md

# Welcome to Minewar-Utils Wiki! 🎮

**Minewar-Utils** is a high-performance Paper plugin designed to provide essential server utilities, advanced chat management, world configuration tools, and quality-of-life commands for Minecraft servers.

---

## 🌟 Key Features

### Core Commands
- **Teleportation**: `/top`, `/bottom`, `/spawn` with customizable spawn points
- **Player Management**: `/heal`, `/eat`, `/fly`, `/flyspeed` for server moderation
- **Gamemode Shortcuts**: `/gmc`, `/gms`, `/gma`, `/gmsp` for quick gamemode switching
- **Inventory Tools**: `/inventorysee`, `/clearinventory`, `/give`, `/head`

### Chat Management
- **Global Chat Control**: Mute, slow mode, and clear chat functionality
- **Player Mentions**: @mention system with customizable formatting and sounds
- **Clickable Links**: Automatic URL detection and formatting
- **Group-Based Formats**: Different chat formats per LuckPerms group

### Build Mode System
- **Restricted Building**: Players can only break blocks they've placed
- **Admin Bypass**: `/bmadmin` for staff to bypass restrictions
- **Block Tracking**: Reset placed blocks with `/bmreset`

### World Configuration
- **Per-World Settings**: Configure PvP, block breaking/placing, mob spawning
- **Anti-Void Protection**: Automatic teleportation when falling below Y-level
- **Block Decay Control**: Manage 0-tick farms and block decay

### Advanced Features
- **Paginated Help**: `/mu help [page]` with customizable format
- **Smart Permissions**: Permission-aware command filtering
- **Tab-Complete Control**: Restrict command suggestions for non-admins
- **Join Commands**: Execute commands when players join

---

## 📚 Quick Links

- **Installation** - Get started with Minewar-Utils
- **Commands and Permissions** - Complete command reference
- **Configuration** - Detailed config.yml guide
- **Messages Configuration** - Customize all plugin messages
- **World Flags** - Per-world settings documentation
- **Build Mode** - Build mode system guide
- **Chat System** - Chat formatting and mentions

---

## 🔧 Quick Start

1. Download the latest release
2. Place `Minewar-Utils-1.0.jar` in your `plugins/` folder
3. Restart your server
4. Configure `config.yml`, `messages.yml`, and `permissions.yml`
5. Run `/mu reload` to apply changes

---

## 💡 Need Help?

- Check the Commands and Permissions page for command usage
- Review the Configuration page for setup options
- Join our Discord for support (link in README)

---

## 📝 Version Information

- **Current Version**: 1.0
- **API Version**: 1.20+
- **Authors**: Jxsh, Jzsh

---

Built with ❤️ by Jxsh & Jzsh | [GitHub Repository](https://github.com/yourusername/minewar-utils)

---

# 📄 _Sidebar.md

### 📖 Navigation

**Getting Started**
- Home
- Installation

**Documentation**
- Commands and Permissions
- Configuration
- Messages Configuration

**Features**
- World Flags
- Build Mode
- Chat System

**Advanced**
- Permissions Guide
- Troubleshooting

---

**Version**: 1.0  
**Authors**: Jxsh & Jzsh

---

# 📄 Installation.md

# Installation Guide

This page provides step-by-step instructions for installing and configuring Minewar-Utils on your Minecraft server.

---

## 📋 Requirements

### Server Requirements
- **Server Software**: Paper 1.20+ (or Spigot 1.20+)
- **Java Version**: Java 17 or higher
- **Minecraft Version**: 1.20+

### Recommended Specifications
- **RAM**: Minimum 2GB allocated to server
- **CPU**: Multi-core processor recommended
- **Storage**: 100MB free space for plugin and data

---

## 🔌 Dependencies

### Hard Dependencies
None! Minewar-Utils works standalone.

### Soft Dependencies (Optional)
The plugin will automatically hook into these if present:

| Plugin | Purpose | Required? |
|--------|---------|-----------|
| **LuckPerms** | Advanced permissions, group-based chat formats | No |
| **PlaceholderAPI** | Placeholder support in messages | No |
| **ProtocolLib** | Advanced packet manipulation | No |
| **Velocitab** | Tab list integration | No |
| **WorldEdit/FAWE** | World editing integration | No |
| **AxiomPaper** | Axiom editor support | No |

> **Note**: While these plugins are optional, LuckPerms is highly recommended for the best experience with group-based chat formatting.

---

## 📥 Installation Steps

### 1. Download the Plugin

Download the latest `Minewar-Utils-1.0.jar` from:
- [GitHub Releases](https://github.com/yourusername/minewar-utils/releases)
- [SpigotMC](https://www.spigotmc.org/resources/)

### 2. Install the Plugin

1. Stop your server if it's running
2. Place `Minewar-Utils-1.0.jar` in your server's `plugins/` folder
3. Start your server

### 3. Verify Installation

Check your console for:
```
[Minewar-Utils] Enabling Minewar-Utils v1.0
[Minewar-Utils] Plugin enabled successfully!
```

Run `/plugins` in-game to verify the plugin is loaded (should show green).

### 4. Initial Configuration

After first startup, the plugin will generate:
```
plugins/Minewar-Utils/
├── config.yml          # Main configuration
├── messages.yml        # All plugin messages
├── permissions.yml     # Permission mappings
├── scoreboard.yml      # Scoreboard settings
└── buildmode_data.json # Build mode data (auto-generated)
```

---

## ⚙️ Basic Configuration

### Set Spawn Location

1. Stand at your desired spawn point
2. Run `/setspawn`
3. Spawn is now saved!

### Configure Permissions

Edit `permissions.yml` to customize permission nodes:

```yaml
commands:
  spawn:
    node: "minewar.spawn"
    default: true  # All players can use /spawn
```

### Enable/Disable Features

Edit `config.yml` to toggle features:

```yaml
features:
  spawn: true
  buildmode: true
  world-flags: true
```

---

## 🔄 Reloading Configuration

After making changes to any config file:

```
/mu reload
```

Or restart your server for a full reload.

---

## ✅ Post-Installation Checklist

- [ ] Plugin shows as enabled in `/plugins`
- [ ] `/mu` command works
- [ ] Spawn location is set with `/setspawn`
- [ ] Permissions are configured for your groups
- [ ] Chat formatting is customized (if using LuckPerms)
- [ ] Feature toggles are set in `config.yml`

---

## 🆘 Troubleshooting

### Plugin Won't Load

**Check Java version:**
```bash
java -version
```
Must be Java 17 or higher.

**Check server software:**
Ensure you're running Paper 1.20+ or Spigot 1.20+.

### Commands Not Working

1. Check if the feature is enabled in `config.yml`
2. Verify permissions in `permissions.yml`
3. Run `/mu reload` after config changes

### Permission Issues

If using LuckPerms:
```
/lp user <player> permission set minewar.spawn true
```

---

## 📚 Next Steps

- Commands and Permissions - Learn all available commands
- Configuration - Detailed configuration guide
- Chat System - Set up custom chat formatting

---

Built with ❤️ by Jxsh & Jzsh | [GitHub Repository](https://github.com/yourusername/minewar-utils)

---

# 📄 Commands-and-Permissions.md

# Commands and Permissions

Complete reference for all Minewar-Utils commands, their permissions, and default access levels.

---

## 📋 Command Overview

Minewar-Utils provides **40+ commands** across multiple categories. Use `/mu help [page]` in-game for a paginated command list.

---

## 🎮 MinewarUtils Main Command

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/minewarutils` | Display plugin header | - | Everyone |
| `/mu help [page]` | Show paginated help (10 commands per page) | - | Everyone |
| `/mu reload` | Reload all configuration files | `minewar.admin.reload` | OP |
| `/mu sneak <enable\|disable> [player]` | Toggle sneak effects | `minewar.admin.sneak` | OP |
| `/mu world [world] <setting> <value>` | Configure world settings | `minewar.world.flags` | OP |

**Aliases**: `/mu`, `/jxsh`

---

## 💬 Chat Management

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/mutechat` | Toggle global chat mute | `minewar.mutechat.toggle` | OP |
| `/slowchat <seconds\|off>` | Set chat slow mode (cooldown between messages) | `minewar.slowchat.set` | OP |
| `/clearchat` | Clear chat for all players | `minewar.clearchat` | OP |
| `/mentiontoggle` | Toggle receiving @mentions | `minewar.mentiontoggle` | Everyone |

**Aliases**:
- `/mutechat` → `/mc`
- `/slowchat` → `/slc`
- `/clearchat` → `/cc`
- `/mentiontoggle` → `/mt`, `/togglementions`

### Chat Bypass Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `minewar.mutechat.bypass` | Speak when chat is muted | OP |
| `minewar.slowchat.bypass` | Bypass slow chat cooldown | OP |
| `minewar.clearchat.bypass` | Prevent chat from being cleared | OP |
| `minewar.mentions` | Ability to @mention other players | Everyone |

---

## 🚀 Teleportation Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/top` | Teleport to highest block above you | `minewar.top` | OP |
| `/bottom` | Teleport to lowest safe block below you | `minewar.bottom` | OP |
| `/spawn` | Teleport to spawn location | `minewar.spawn` | Everyone |
| `/setspawn` | Set spawn location at your position | `minewar.setspawn` | OP |

---

## 👤 Player Management

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/heal [player]` | Heal yourself or another player | `minewar.heal` | OP |
| `/eat [player]` | Feed yourself or another player | `minewar.eat` | OP |
| `/fly [player]` | Toggle flight mode | `minewar.fly` | OP |
| `/flyspeed <0-10> [player]` | Set flight speed (0=slowest, 10=fastest) | `minewar.flyspeed` | OP |

**Aliases**:
- `/eat` → `/feed`

---

## 🎮 Gamemode Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/gamemode <mode> [player]` | Change gamemode | `minewar.gamemode` | OP |
| `/gmc [player]` | Set creative mode | `minewar.gamemode.creative` | OP |
| `/gms [player]` | Set survival mode | `minewar.gamemode.survival` | OP |
| `/gma [player]` | Set adventure mode | `minewar.gamemode.adventure` | OP |
| `/gmsp [player]` | Set spectator mode | `minewar.gamemode.spectator` | OP |

**Aliases**:
- `/gamemode` → `/gm`
- `/gmc` → `/creative`
- `/gms` → `/survival`
- `/gma` → `/adventure`
- `/gmsp` → `/spectator`, `/spectate`

---

## 🎒 Inventory Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/inventorysee <player> [1\|2]` | View player's inventory (1=main, 2=armor) | `minewar.inventorysee` | OP |
| `/clearinventory [player]` | Clear inventory | `minewar.clearinventory` | OP |
| `/give <item> [amount] [player]` | Give items to yourself or others | `minewar.give` | OP |
| `/head <player>` | Get a player's head | `minewar.head` | OP |

**Aliases**:
- `/inventorysee` → `/invsee`, `/inventory`
- `/clearinventory` → `/clear`, `/clearinv`, `/ci`
- `/give` → `/item`, `/i`
- `/head` → `/skull`, `/playerhead`

---

## 🏗️ Build Mode System

Build Mode restricts players to only breaking blocks they've placed themselves.

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/buildmode [player]` | Toggle build mode for yourself or another player | `minewar.buildmode` | OP |
| `/bmadmin [player]` | Toggle admin bypass (break any block) | `minewar.buildmode.admin` | OP |
| `/bmreset [player]` | Reset all placed blocks for a player | `minewar.buildmode.reset` | OP |

**Aliases**:
- `/buildmode` → `/bm`
- `/bmadmin` → `/bma`
- `/bmreset` → `/bmr`

### Build Mode Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `minewar.buildmode` | Use /buildmode command | OP |
| `minewar.buildmode.others` | Toggle buildmode for other players | OP |
| `minewar.buildmode.admin` | Use /bmadmin command | OP |
| `minewar.buildmode.reset` | Use /bmreset command | OP |
| `minewar.buildmode.bypass` | Bypass buildmode restrictions globally | OP |

---

## 🎉 Utility Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/poopgun [player]` | Give yourself or another player a poop gun | `minewar.poopgun` | OP |
| `/devarmour [player]` | Toggle rainbow leather armor | `minewar.dev.armour` | OP |

**Aliases**:
- `/devarmour` → `/devarmor`

---

## 🌍 World Flags

Configure per-world settings with `/mu world`:

```
/mu world [world] <setting> <value>
```

### Available Settings

| Setting | Values | Description |
|---------|--------|-------------|
| `destroy` | true/false | Allow block breaking |
| `place` | true/false | Allow block placing |
| `pvp` | true/false | Enable PvP combat |
| `invincible` | true/false | Players take no damage |
| `spawnmobs` | true/false | Allow mob spawning |
| `nomobs` | true/false | Prevent all mobs |
| `antivoid` | true <level> / false | Teleport players above Y-level |
| `blockdecay` | true/false | Allow block decay |
| `0tick` | true/false | Allow 0-tick farms |

**Example**:
```
/mu world world_nether pvp false
/mu world world antivoid true -64
```

---

## 🔐 Admin Permissions

Special permissions for advanced features:

| Permission | Description |
|------------|-------------|
| `minewar.admin.tabcomplete` | Bypass tab-completion restrictions |
| `minewar.vanish.see` | See vanished players |
| `minewar.vanish.admins` | Hide from standard staff |
| `minewar.vanish.admins.see` | See admin-vanished players |

---

## 💡 Permission Tips

### Grant All Commands
```
/lp group admin permission set minewar.* true
```

### Grant Specific Category
```
/lp group moderator permission set minewar.gamemode.* true
```

### Default Player Permissions
These permissions are granted to all players by default:
- `minewar.spawn` - Use /spawn
- `minewar.mentiontoggle` - Toggle mentions
- `minewar.mentions` - Mention other players

---

Built with ❤️ by Jxsh & Jzsh | [GitHub Repository](https://github.com/yourusername/minewar-utils)

---

# 📄 Configuration.md

# Configuration Guide

Complete reference for `config.yml` with detailed explanations for every setting.

---

## 📁 Configuration Files

Minewar-Utils uses multiple configuration files:

| File | Purpose |
|------|---------|
| `config.yml` | Main plugin configuration, feature toggles, and settings |
| `messages.yml` | All user-facing messages and text |
| `permissions.yml` | Permission node mappings |
| `scoreboard.yml` | Scoreboard configuration |
| `buildmode_data.json` | Build mode data (auto-generated) |

---

## ⚙️ config.yml Structure

### Config Version

```yaml
config-version: 4
```

**Do not modify this value**. It's used by the plugin to auto-update your config when new versions are released.

---

## 🎛️ Feature Toggles

Enable or disable commands individually:

```yaml
features:
  minewarutils: true
  reload: true
  sneak: true
  mutechat: true
  slowchat: true
  clearchat: true
  mentiontoggle: true
  top: true
  bottom: true
  heal: true
  eat: true
  fly: true
  flyspeed: true
  gamemode: true
  gmc: true
  gms: true
  gma: true
  gmsp: true
  inventorysee: true
  clearinventory: true
  give: true
  head: true
  setspawn: true
  spawn: true
  join-spawn: true  # Teleport to spawn on join
  poopgun: true
  devarmour: true
  buildmode: true  # Includes /buildmode, /bmadmin, /bmreset
  world-flags: true  # Includes /mu world command
```

> **Note**: Setting a feature to `false` will completely disable that command and unregister it from the server.

---

## 🚪 Join Commands

Execute commands when players join the server:

```yaml
Join-Commands:
  enabled: true
  as-console:
    enabled: true
    commands:
      - "clear %player_name%"
```

**Placeholders**:
- `%player_name%` - Player's username
- `%player_uuid%` - Player's UUID
- Any PlaceholderAPI placeholder

**Example**:
```yaml
commands:
  - "clear %player_name%"
  - "effect give %player_name% minecraft:speed 10 1"
  - "tellraw %player_name% {\"text\":\"Welcome!\",\"color\":\"gold\"}"
```

---

## 📜 MOTD Configuration

```yaml
MOTD:
  enabled: true
```

MOTD messages are configured in `messages.yml`.

---

## ⌨️ Tab-Complete Settings

Control which commands appear in tab-completion for non-admin players:

```yaml
tab-complete:
  allowed-commands:
    - "spawn"
    - "mu"
    - "help"
```

**How it works**:
- Players **without** `minewar.admin.tabcomplete` permission only see these commands
- Players **with** the permission see all commands
- Set to `[]` to hide ALL commands from non-admins

---

## 💬 Join/Leave Messages

```yaml
join-messages:
  enabled: true
  custom-message:
    enabled: false
```

Messages are configured in `messages.yml`:
```yaml
join-quit:
  join-format: "<green>+ <gray>%player%"
  quit-format: "<red>- <gray>%player%"
```

---

## 🔤 Command Aliases

Customize command aliases:

```yaml
aliases:
  minewarutils: [mu, jxsh]
  mutechat: [mc]
  slowchat: [slc]
  clearchat: [cc]
  mentiontoggle: [mt, togglementions]
  eat: [feed]
  gamemode: [gm]
  inventorysee: [invsee, inventory]
  clearinventory: [clear, clearinv, ci]
  give: [item, i]
  head: [skull, playerhead]
  gmc: [creative]
  gms: [survival]
  gma: [adventure]
  gmsp: [spectator, spectate]
```

**To disable aliases**: Set to empty list `[]`

---

## 👟 Sneak Effect Settings

Configure special effects when specific players sneak:

```yaml
enabled-users:
  - Jxsh

drop-item-type: DIAMOND
drop-item-name: "<white>%luckperms_suffix%%player_name%'s"
drop-item-enabled: false
drop-duration-seconds: 3

particle-type: HEART
particle-count: 1
particle-duration-seconds: 1
```

**Settings**:
- `enabled-users` - List of players who trigger sneak effects
- `drop-item-type` - Material that drops (e.g., DIAMOND, GOLD_INGOT)
- `drop-item-name` - Custom name for dropped item (supports MiniMessage)
- `drop-item-enabled` - Whether items actually drop
- `drop-duration-seconds` - How long item stays on ground
- `particle-type` - Particle effect (HEART, FLAME, etc.)
- `particle-count` - Number of particles
- `particle-duration-seconds` - How long particles last

---

## 📍 Spawn Location

```yaml
spawn:
  world: "world"
  x: 0.0
  y: 64.0
  z: 0.0
  yaw: 0.0
  pitch: 0.0
```

> **Tip**: Use `/setspawn` in-game instead of manually editing this.

---

## 💬 Chat System

### Basic Settings

```yaml
chat:
  enabled: true
  clickable-links: true
  link-format: "<yellow><bold><underlined>%link%"
```

**Settings**:
- `enabled` - Enable/disable chat formatting
- `clickable-links` - Auto-detect and make URLs clickable
- `link-format` - Format for clickable links (supports MiniMessage)

### Chat Formats

```yaml
  format: <white>%luckperms_prefix%<gray>%player% <dark_gray>»</dark_gray> <#ccffff>%message%
  
  group-formats:
    Owner: <white>%luckperms_prefix%<gray>%player% <dark_gray>»</dark_gray> <#0adef7>%message%
```

**Placeholders**:
- `%player%` - Player name
- `%message%` - Chat message
- `%luckperms_prefix%` - LuckPerms prefix
- `%luckperms_suffix%` - LuckPerms suffix
- Any PlaceholderAPI placeholder

**Group Formats**:
- Key must match LuckPerms primary group name
- Falls back to default `format` if group not found

### Mentions

```yaml
  mentions:
    enabled: true
    format: "%suffix_other%%player%"
    sound: "minecraft:entity.villager.ambient"
    volume: 1.0
    pitch: 1.0
```

**Settings**:
- `enabled` - Enable @mention system
- `format` - How mentions appear in chat
- `sound` - Sound played to mentioned player
- `volume` - Sound volume (0.0-1.0)
- `pitch` - Sound pitch (0.0-2.0)

**Format Placeholders**:
- `%player%` - Mentioned player's name
- `%suffix_other%` - Mentioned player's LuckPerms suffix

---

## 🏗️ Build Mode

```yaml
buildmode:
  enabled: true
  database-file: "buildmode_data.json"
  deny-global-build: true
  send-deny-message: true
```

**Settings**:
- `enabled` - Enable build mode system
- `database-file` - File to store placed block data
- `deny-global-build` - If true, NO ONE can build unless in buildmode
- `send-deny-message` - Show message when build is denied

> **Important**: When `deny-global-build: true`, players MUST use `/buildmode` to build anything.

---

## 🌍 World Flags

Default settings for all worlds:

```yaml
world-flags:
  destroy: true
  place: true
  pvp: true
  invincible: false
  spawnmobs: true
  nomobs: false
  antivoid: true
  antivoid-level: -64
  blockdecay: false
  0tick: false
```

**Settings**:
- `destroy` - Allow block breaking
- `place` - Allow block placing
- `pvp` - Enable PvP combat
- `invincible` - Players take no damage
- `spawnmobs` - Allow mob spawning
- `nomobs` - Prevent all mobs
- `antivoid` - Teleport players when falling below level
- `antivoid-level` - Y-level to trigger teleport
- `blockdecay` - Allow block decay (leaves, etc.)
- `0tick` - Allow 0-tick farms

### Per-World Overrides

```yaml
worlds: {}
```

This section is auto-generated when you use `/mu world`. Example:

```yaml
worlds:
  world_nether:
    pvp: false
    antivoid: true
    antivoid-level: -64
```

---

## 🎉 Utility Features

### Poop Gun

```yaml
utility:
  poopgun:
    item:
      material: "DIAMOND_HOE"
      name: "<gold><b>Poop Gun"
      lore:
        - "<gray>Right-click to make someone poop!"
```

**Settings**:
- `material` - Item material (any valid Minecraft material)
- `name` - Display name (supports MiniMessage)
- `lore` - Item lore lines (supports MiniMessage)

---

## 🔄 Reloading Configuration

After editing any config file:

```
/mu reload
```

This reloads:
- `config.yml`
- `messages.yml`
- `permissions.yml`
- `scoreboard.yml`

> **Note**: Some changes (like feature toggles) require a server restart.

---

Built with ❤️ by Jxsh & Jzsh | [GitHub Repository](https://github.com/yourusername/minewar-utils)

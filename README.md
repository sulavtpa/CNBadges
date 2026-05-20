# CN-Badges

CN-Badges was created to fullfill the specific requirement of having a sustainable, easy yet feature rich **badges** that is avaiable and viewable on multiple servers.
*ofc it needs to share the same database*

## Features

- Make any number of badge types with N tiers.
- Assign badges to players by name.
- Per-player "special badge" with configurable symbol and colour.
- Small per-player cache (approx. 600–700 bytes per online player) thats cleared after they disconnect.
- Async database operations
- PlaceholderAPI support.
- Hot‑reload support for config and db.
- Minimessage Formatting SUpport.

## Requirements

- **Java 21** or newer
- **Paper 1.21.4** or a compatible fork (the plugin uses Paper API)
- A **MySQL** or **MariaDB** database
- **PlaceholderAPI** (required for the `%cnbadges%` placeholder)

### Building from source

```bash
mvn clean package
```

## Configuration

All plugin settings are stored in `plugins/CN-BadgeSystem/config.toml`. The file is written in TOML and contains two sections:

```toml
[database]
host = "127.0.0.1"
port = 3306
database = "badges_db"
username = "root"
password = "password"
```

Example:

```toml
[badges.donor]
weight = 10
tiers = { 1 = "<yellow>★</yellow>", 2 = "<gold>★★</gold>" }

[badges.staff]
weight = 100
tiers = { 1 = "<red>[Mod]</red>", 2 = "<dark_red>[Admin]</dark_red>" }
```

After editing the file, use `/badge reload` to apply changes.

## Commands

### `/badge` (alias `/badges`)

All subcommands require the permission `cn.badges.admin`.

| Command | Description |
|---------|-------------|
| `/badge create <name> <display>` | Create a new badge type with a single tier 1. |
| `/badge tier <name> <tier> <display>` | Add or update a specific tier for an existing badge. |
| `/badge edit <name> tier <tier> <display>` | Edit the display of an existing tier (same as `tier`). |
| `/badge give <player> <badge> [tier]` | Give a badge to a player; tier defaults to 1. |
| `/badge remove <player> <badge>` | Remove a specific badge from a player. |
| `/badge remove <badge>` | Delete a badge type entirely (from all players and config). |
| `/badge remove <badge> tier <tier>` | Remove a single tier from a badge definition. |
| `/badge special give <player>` | Grant a player permission to use `/mybadge`. |
| `/badge reload` | Reload configuration and reconnect to the database. |

Tab completion is available for all subcommands and their arguments.

### `/mybadge`

This command is available to any player who has been granted special badge permission via `/badge special give`.
It does not require an additional permission node. A 5‑second cooldown applies to prevent spam.

Usage:  
`/mybadge <symbol> <color>`

Available symbols: `✧` `✦` `★`  
Available colours (16 standard Minecraft colour names):  
`black`, `dark_gray`, `dark_blue`, `blue`, `dark_green`, `green`, `dark_aqua`, `aqua`, `dark_red`, `red`, `dark_purple`, `light_purple`, `gold`, `yellow`, `gray`, `white`

The chosen symbol and colour are saved in the database and will appear in the placeholder output and wherever badges are shown. If a player already has a special badge, the new choice replaces the old one immediately.

## PlaceholderAPI

It registers `%cnbadges%`, displaying a player's special badge first, then normal badges by weight if needed.
If the player has no badges or is offline, the placeholder returns an empty string.

## Caching and Performance

The badge data for players who are online is kept in memory so that the database does not have to be queried each time there is a chat message or scoreboard update.
When a player connects, their badges are fetched and kept in the cache asynchronously. When they disconnect, after a certain amount of time (200 ticks, or 10 seconds), their cache is cleaned. But if the player reconnects within this period, the task is canceled.

**Per-player memory footprint (approximate):**

| Component | Type | Approx. size (bytes) |
|-----------|------|-----------------------|
| UUID key (in each map) | `java.util.UUID` | 24 |
| Badges map entry | `ConcurrentHashMap` node | 48 |
| Each badge entry (average 5 per player) | `Map.Entry<String, Integer>` | ~80 |
| Special badge data + map entry | `SpecialBadgeData` + node | ~120 |
| Removal task ID | `Map.Entry<UUID, Integer>` | ~72 |
| **Total (5 normal badges + special)** | | **~600–700 bytes** |

This conservative estimate means that even with hundreds of concurrent players, the cache adds minimal memory pressure.

## Diagrams

### Class Diagram
![Class diagram](https://github.com/user-attachments/assets/24448cae-ba97-41b9-bdb0-d8a293a5b4d2)

### Sequence Diagrams

**Player join / initial loading**
![Join sequence](https://github.com/user-attachments/assets/f8796f5b-dd43-44d2-b7c3-0c062273fc00)

**/mybadge flow**
![MyBadge sequence](https://github.com/user-attachments/assets/25aa4244-02e0-4d2b-b025-f5cc1823ca3d)

## License

CN-Badges is released under the **GNU General Public License v2.0**. See the [LICENSE](./LICENSE) file for the full text.

---

*This plugin is made for CraftNepal minecraft server play.craftnepal.net :) Feel free to join, Everyone is welcome*

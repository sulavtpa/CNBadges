# CN-BadgeSystem

A modern, high-performance Minecraft Paper plugin for managing and displaying customizable player badges.

## Features

- **MySQL Database Integration:** Stores player badge assignments efficiently.
- **Customizable Badges:** Configure badge tiers and display text using MiniMessage format.
- **Commands:** Administrative commands to create, edit, delete, and list badges, as well as assigning them to players.
- **Caching:** MapDB based fast cache to ensure the server thread is never blocked.
- **Modern Text Formatting:** Fully utilizes the Adventure API for text formatting.

## Configuration

Configure your database and badges in `plugins/CNBadges/config.toml`:

```toml
[database]
host = "127.0.0.1"
port = 3306
database = "badges_db"
username = "root"
password = "password"

# Example Badges
# [badges.donor]
# weight = 10
# tiers = { 1 = "<yellow>★</yellow>", 2 = "<gold>★★</gold>" }
```

## Setup & Compilation

1. Ensure you have Java 21 and Maven installed.
2. Clone the repository.
3. Run `mvn clean package`.
4. The compiled `.jar` file will be in the `target/` directory. Drop it into your server's `plugins` folder.

## Commands

- `/badges help` - Show command help.
- `/badges give <player> <badge> <tier>` - Give a player a badge.
- `/badges remove <player> <badge>` - Remove a badge from a player.
- `/badges list` - List all configured badges.
- `/badges reload` - Reload the plugin configuration and database connection.

## Requirements

- PaperMC server (1.20+)
- Java 21+
- MySQL Server

## License

This project is licensed under the MIT License.

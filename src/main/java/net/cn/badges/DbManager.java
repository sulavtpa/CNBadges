package net.cn.badges;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DbManager {
    private final CNBadges plugin;
    private HikariDataSource dataSource;
    private org.bukkit.scheduler.BukkitTask connectionTask;

    public DbManager(CNBadges plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        if (connectionTask != null)
            connectionTask.cancel();
        connectionTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, this::attemptConnection);
    }

    private void attemptConnection() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + plugin.getCfgManager().getDbHost() + ":" +
                    plugin.getCfgManager().getDbPort() + "/" + plugin.getCfgManager().getDbName() +
                    "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true");
            config.setUsername(plugin.getCfgManager().getDbUser());
            config.setPassword(plugin.getCfgManager().getDbPassword());
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(5000);
            config.setInitializationFailTimeout(1);

            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Connecting to database at " + plugin.getCfgManager().getDbHost() + ":"
                    + plugin.getCfgManager().getDbPort() + "...");
            createTable();
            plugin.getLogger().info("Successfully connected to the database.");

            Bukkit.getOnlinePlayers().forEach(p -> loadplayer(p.getUniqueId()));

        } catch (Exception e) {
            plugin.getLogger()
                    .warning("Failed to connect to database: " + e.getMessage());
            dataSource = null;
        }
    }

    public void reload() {
        disconnect();
        connect();
    }

    private void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS cn_badges (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY," +
                                "uuid VARCHAR(36) NOT NULL," +
                                "badge_id VARCHAR(64) NOT NULL," +
                                "tier INT DEFAULT 1," +
                                "UNIQUE KEY unique_user_badge (uuid, badge_id)" +
                                ");");
                PreparedStatement psSpecial = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS cn_special_badges (" +
                                "uuid VARCHAR(36) PRIMARY KEY," +
                                "has_permission BOOLEAN DEFAULT FALSE," +
                                "symbol VARCHAR(16)," +
                                "color VARCHAR(32)" +
                                ");")) {
            ps.executeUpdate();
            psSpecial.executeUpdate();
        }
    }

    public void disconnect() {
        if (connectionTask != null) {
            connectionTask.cancel();
            connectionTask = null;
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    public void loadplayer(UUID uuid) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT badge_id, tier FROM cn_badges WHERE uuid = ?");
                    PreparedStatement psSpecial = conn.prepareStatement(
                            "SELECT has_permission, symbol, color FROM cn_special_badges WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                Map<String, Integer> badges = new HashMap<>();
                while (rs.next()) {
                    badges.put(rs.getString("badge_id"), rs.getInt("tier"));
                }
                plugin.getBadgeCache().loadplayer(uuid, badges);

                psSpecial.setString(1, uuid.toString());
                ResultSet rsSpecial = psSpecial.executeQuery();
                if (rsSpecial.next()) {
                    plugin.getBadgeCache().loadSpecialBadge(uuid,
                            new SpecialBadgeData(rsSpecial.getBoolean("has_permission"), rsSpecial.getString("symbol"),
                                    rsSpecial.getString("color")));
                } else {
                    plugin.getBadgeCache().loadSpecialBadge(uuid, new SpecialBadgeData(false, null, null));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load badges for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void giveBadge(UUID uuid, String badge, int tier) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO cn_badges (uuid, badge_id, tier) VALUES (?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE tier = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, badge);
                ps.setInt(3, tier);
                ps.setInt(4, tier);
                ps.executeUpdate();
                plugin.getBadgeCache().addBadge(uuid, badge, tier);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to give badge to " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void removeBadge(UUID uuid, String badge) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM cn_badges WHERE uuid = ? AND badge_id = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, badge);
                ps.executeUpdate();
                plugin.getBadgeCache().remBadge(uuid, badge);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to remove badge from " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void removeBadgeFromAll(String badge) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM cn_badges WHERE badge_id = ?")) {
                ps.setString(1, badge);
                ps.executeUpdate();
                Bukkit.getOnlinePlayers().forEach(p -> plugin.getBadgeCache().remBadge(p.getUniqueId(), badge));
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to remove badge " + badge + " from all users: " + e.getMessage());
            }
        });
    }

    public void giveSpecialPermission(UUID uuid) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO cn_special_badges (uuid, has_permission) VALUES (?, true) ON DUPLICATE KEY UPDATE has_permission = true")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                SpecialBadgeData data = plugin.getBadgeCache().getSpecialBadge(uuid);
                if (data != null) {
                    data.setPermission(true);
                } else {
                    plugin.getBadgeCache().loadSpecialBadge(uuid, new SpecialBadgeData(true, null, null));
                }
            } catch (SQLException e) {
                plugin.getLogger()
                        .warning("Failed to give special badge permission to " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void setSpecialBadge(UUID uuid, String symbol, String color) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE cn_special_badges SET symbol = ?, color = ? WHERE uuid = ?")) {
                ps.setString(1, symbol);
                ps.setString(2, color);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
                SpecialBadgeData data = plugin.getBadgeCache().getSpecialBadge(uuid);
                if (data != null) {
                    data.setSymbol(symbol);
                    data.setColor(color);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to set special badge for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void removeSpecialPermission(UUID uuid) {
        if (dataSource == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM cn_special_badges WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                SpecialBadgeData data = plugin.getBadgeCache().getSpecialBadge(uuid);
                if (data != null) {
                    data.setPermission(false);
                    data.setSymbol(null);
                    data.setColor(null);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to remove sbadge for " + uuid + ": " + e.getMessage());
            }
        });
    }
}
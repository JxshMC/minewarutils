package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final JxshMisc plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(JxshMisc plugin) {
        this.plugin = plugin;
        connect();
    }

    private boolean isConnected = false;

    public boolean isConnected() {
        return isConnected && dataSource != null && !dataSource.isClosed();
    }

    private void connect() {
        // 1. Path Safety (Sync)
        new java.io.File(plugin.getDataFolder(), "Database").mkdirs();

        // 2. Async Initialization
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String type = plugin.getConfigManager().getConfig().getString("storage.type", "H2").toUpperCase();
            HikariConfig config = new HikariConfig();

            // 1. Storage Type Detection & Specific URL
            if (type.equals("MARIADB")) {
                String host = plugin.getConfigManager().getConfig().getString("storage.host");
                String port = plugin.getConfigManager().getConfig().getString("storage.port");
                String database = plugin.getConfigManager().getConfig().getString("storage.database");
                String username = plugin.getConfigManager().getConfig().getString("storage.username");
                String password = plugin.getConfigManager().getConfig().getString("storage.password");

                config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
                config.setUsername(username);
                config.setPassword(password);
            } else {
                // IF H2: relative path with ./ mandatory
                config.setJdbcUrl(
                        "jdbc:h2:file:./plugins/MinewarUtils/Database/minewarutils;MODE=MySQL;AUTO_SERVER=TRUE");
                config.setDriverClassName("org.h2.Driver");
                // 4. Hikari Tuning for H2
                config.setConnectionTimeout(5000);
            }

            config.setMaximumPoolSize(
                    plugin.getConfigManager().getConfig().getInt("storage.pool-settings.maximum-pool-size", 10));
            config.setMinimumIdle(
                    plugin.getConfigManager().getConfig().getInt("storage.pool-settings.minimum-idle", 10));
            config.setMaxLifetime(
                    plugin.getConfigManager().getConfig().getInt("storage.pool-settings.max-lifetime", 1800000));
            if (!type.equals("H2")) {
                // Apply timeout for MariaDB too if not set? Config has default
                config.setConnectionTimeout(
                        plugin.getConfigManager().getConfig().getInt("storage.pool-settings.connection-timeout", 5000));
            }

            try {
                dataSource = new HikariDataSource(config);
                // Test connection
                try (Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(1)) {
                        plugin.getLogger().info("Database connected successfully! (" + type + ")");
                        isConnected = true;
                        initTables();
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to connect to " + type + " database!");
                plugin.getLogger().severe("Falling back to local storage (YAML/JSON) for data persistence.");
                // Ensure dataSource is nullified if it was partially created (though Hikari
                // usually checks config first)
                if (dataSource != null) {
                    dataSource.close();
                    dataSource = null;
                }
                e.printStackTrace();
            }
        });
    }

    private void initTables() {
        if (!isConnected())
            return;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // BuildMode Table
            stmt.execute("CREATE TABLE IF NOT EXISTS buildmode_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "blocks_placed INT DEFAULT 0, " +
                    "active BOOLEAN DEFAULT FALSE" +
                    ");");

            // TempOp Table
            stmt.execute("CREATE TABLE IF NOT EXISTS temp_op (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "duration BIGINT, " +
                    "start_time BIGINT" +
                    ");");

        } catch (SQLException e) {
            plugin.getLogger().severe("Database initialization failed!");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is null (Database not connected)");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            try {
                if (!dataSource.isClosed()) {
                    dataSource.close();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing DataSource: " + e.getMessage());
            } finally {
                dataSource = null;
                isConnected = false;
            }
        }
    }
}

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

    private void connect() {
        String type = plugin.getConfigManager().getConfig().getString("storage.type", "H2").toUpperCase();

        HikariConfig config = new HikariConfig();

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
            // Default H2
            // Use DB_CLOSE_DELAY=0 to ensure it closes immediately when the last connection
            // is
            // closed.
            // Use AUTO_SERVER=TRUE to allow mixed mode (though less relevant with
            // CLOSE_DELAY=0, it's safer for reloads).
            config.setJdbcUrl(
                    "jdbc:h2:./plugins/MinewarUtils/Database/minewarutils;MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=0");
            config.setDriverClassName("org.h2.Driver");
        }

        config.setMaximumPoolSize(
                plugin.getConfigManager().getConfig().getInt("storage.pool-settings.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfigManager().getConfig().getInt("storage.pool-settings.minimum-idle", 10));
        config.setMaxLifetime(
                plugin.getConfigManager().getConfig().getInt("storage.pool-settings.max-lifetime", 1800000));
        config.setConnectionTimeout(
                plugin.getConfigManager().getConfig().getInt("storage.pool-settings.connection-timeout", 5000));

        // Retry logic for H2 file locks during reloads
        int attempts = 5; // Increased to 5
        for (int i = 0; i < attempts; i++) {
            try {
                dataSource = new HikariDataSource(config);
                // Test connection
                try (Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(1)) {
                        plugin.getLogger().info("Database connected successfully!");
                        break;
                    }
                }
            } catch (Exception e) {
                if (i == attempts - 1) {
                    plugin.getLogger().severe("Failed to connect to database after " + attempts + " attempts!");
                    e.printStackTrace();
                    // Fallback or disable?
                    // If we return, dataSource is null/invalid.
                    return;
                } else {
                    plugin.getLogger().warning("Database lock detected, retrying in 2 seconds... (Attempt " + (i + 1)
                            + "/" + attempts + ")");
                    if (dataSource != null && !dataSource.isClosed()) {
                        dataSource.close();
                    }
                    try {
                        Thread.sleep(2000); // Increased to 2 seconds
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        if (dataSource != null && !dataSource.isClosed()) {
            initTables();
        }
    }

    private void initTables() {
        if (dataSource == null || dataSource.isClosed())
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
            }
        }
    }
}

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
        String type = plugin.getConfigManager().getConfig().getString("storage.type", "H2");

        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("MARIADB")) {
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
            config.setJdbcUrl("jdbc:h2:./plugins/MinewarUtils/Database/minewarutils;MODE=MySQL");
            config.setDriverClassName("org.h2.Driver");
        }

        config.setMaximumPoolSize(
                plugin.getConfigManager().getConfig().getInt("storage.pool-settings.maximum-pool-size", 10));
        config.setMinimumIdle(plugin.getConfigManager().getConfig().getInt("storage.pool-settings.minimum-idle", 10));
        config.setMaxLifetime(
                plugin.getConfigManager().getConfig().getInt("storage.pool-settings.max-lifetime", 1800000));
        config.setConnectionTimeout(
                plugin.getConfigManager().getConfig().getInt("storage.pool-settings.connection-timeout", 5000));

        dataSource = new HikariDataSource(config);

        initTables();
    }

    private void initTables() {
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
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

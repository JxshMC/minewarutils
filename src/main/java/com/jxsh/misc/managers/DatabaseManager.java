package com.jxsh.misc.managers;

import com.jxsh.misc.JxshMisc;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class DatabaseManager {

    private final JxshMisc plugin;
    private HikariDataSource dataSource;
    private String type;

    public DatabaseManager(JxshMisc plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        type = plugin.getConfigManager().getConfig().getString("storage.type", "H2").toUpperCase();

        HikariConfig config = new HikariConfig();

        if (type.equals("MARIADB")) {
            String host = plugin.getConfigManager().getConfig().getString("storage.host", "localhost");
            String port = plugin.getConfigManager().getConfig().getString("storage.port", "3306");
            String database = plugin.getConfigManager().getConfig().getString("storage.database", "minewar");
            String username = plugin.getConfigManager().getConfig().getString("storage.username", "root");
            String password = plugin.getConfigManager().getConfig().getString("storage.password", "");

            config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // Default to H2
            File dataFolder = new File(plugin.getDataFolder(), "data");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            config.setJdbcUrl("jdbc:h2:" + dataFolder.getAbsolutePath() + File.separator + "minewarutils;MODE=MySQL");
            config.setDriverClassName("org.h2.Driver");
        }

        config.setPoolName("MinewarUtils-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setMaxLifetime(1800000); // 30 mins

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Database connected successfully (" + type + ")");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        executeUpdates(
                "CREATE TABLE IF NOT EXISTS buildmode_data (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "original_inventory TEXT, " +
                        "original_armor TEXT, " +
                        "original_location TEXT, " +
                        "original_gamemode VARCHAR(20), " +
                        "original_fly BOOLEAN" +
                        ");",

                "CREATE TABLE IF NOT EXISTS temp_ops (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "expires_at BIGINT, " +
                        "is_permanent BOOLEAN" +
                        ");");
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // --- Async Helpers ---

    public CompletableFuture<Void> executeUpdate(String query, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error executing update: " + query);
                e.printStackTrace();
            }
        });
    }

    public void executeUpdates(String... queries) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                for (String query : queries) {
                    try (PreparedStatement ps = conn.prepareStatement(query)) {
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public <T> CompletableFuture<T> executeQuery(String query, Function<ResultSet, T> callback, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return callback.apply(rs);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error executing query: " + query);
                e.printStackTrace();
                return null;
            }
        });
    }
}

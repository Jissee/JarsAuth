package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

public class ConfigDAO implements DAO {
    private final ConnectionProvider provider;

    public ConfigDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }

    public void initValues() {
        for (ConfigKey key : ConfigKey.values()) {
            initKey(key);
        }
    }

    public void initKey(ConfigKey key) {
        String sql = "SELECT COUNT(*) FROM config WHERE key = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, key.getKey());
            try(ResultSet rs = statement.executeQuery()){
                if (rs.next() && rs.getLong(1) == 0) {
                    try (PreparedStatement insertStmt = getConnection().prepareStatement(
                            "INSERT INTO config (key, value) VALUES (?, ?)"
                    )) {
                        insertStmt.setString(1, key.getKey());
                        insertStmt.setLong(2, key.getDefaultValue());
                        insertStmt.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error initializing key: " + key.getKey(), e);
        }
    }

    public long getValue(ConfigKey key) {
        String sql = "SELECT value FROM config WHERE key = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, key.getKey());
            try(ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("value");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting value for key: " + key.getKey(), e);
        }
        return key.getDefaultValue();
    }

    public void setValue(ConfigKey key, long value) {
        String sql = "INSERT INTO config (key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, key.getKey());
            statement.setLong(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error setting value for key: " + key.getKey(), e);
        }
    }

    /**
     * 直接使用 SQL 中的 ORDER BY ordinal 排序
     */
    public TreeMap<String, Long> listAllValues() {
        TreeMap<String, Long> result = new TreeMap<>();
        String sql = "SELECT key, value FROM config";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("key"), rs.getLong("value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing all config values", e);
        }
        return result;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return provider.getConnection();
    }

    @Override
    public String[] getDefSQL() {
        return new String[]{
                """
                CREATE TABLE IF NOT EXISTS config (
                    key TEXT PRIMARY KEY,
                    value INTEGER NOT NULL
                )
                """
        };
    }
}

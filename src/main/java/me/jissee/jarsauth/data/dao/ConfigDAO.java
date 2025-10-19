package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.config.ConfigKey;

import java.sql.*;
import java.util.TreeMap;

public class ConfigDAO implements DAO {
    private final Connection connection;

    public ConfigDAO(Connection connection) {
        this.connection = connection;
        initTable();
    }

    public void initValues() {
        for (ConfigKey key : ConfigKey.values()) {
            initKey(key);
        }
    }

    public void initKey(ConfigKey key) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM config WHERE key = ?"
        )) {
            stmt.setString(1, key.getKey());
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getLong(1) == 0) {
                try (PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO config (key, value) VALUES (?, ?)"
                )) {
                    insertStmt.setString(1, key.getKey());
                    insertStmt.setLong(2, key.getDefaultValue());
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing key: " + key.getKey(), e);
        }
    }

    public long getValue(ConfigKey key) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT value FROM config WHERE key = ?"
        )) {
            stmt.setString(1, key.getKey());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("value");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting value for key: " + key.getKey(), e);
        }
        return key.getDefaultValue();
    }

    public void setValue(ConfigKey key, long value) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO config (key, value) VALUES (?, ?) " +
                        "ON CONFLICT(key) DO UPDATE SET value = excluded.value"
        )) {
            stmt.setString(1, key.getKey());
            stmt.setLong(2, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error setting value for key: " + key.getKey(), e);
        }
    }

    /**
     * 直接使用 SQL 中的 ORDER BY ordinal 排序
     */
    public TreeMap<String, Long> listAllValues() {
        TreeMap<String, Long> result = new TreeMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT key, value FROM config")) {
            while (rs.next()) {
                result.put(rs.getString("key"), rs.getLong("value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error listing all config values", e);
        }
        return result;
    }

    @Override
    public Connection getConnection() {
        return connection;
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

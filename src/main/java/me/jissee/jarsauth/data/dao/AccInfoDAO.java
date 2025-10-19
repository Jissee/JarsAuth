package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.model.AccInfoEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccInfoDAO implements DAO {
    private final Connection connection;

    public AccInfoDAO(Connection connection) {
        this.connection = connection;
        initTable();
    }

    public List<AccInfoEntry> getAllEntries(String groupName) {
        String sql = "SELECT key, value FROM acc_info WHERE group_name = ? ORDER BY `key`";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            List<AccInfoEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AccInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccInfoEntry> getEntriesByKey(String group, String key) {
        String sql = "SELECT key, value FROM acc_info WHERE group_name = ? AND key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group);
            stmt.setString(2, key);
            ResultSet rs = stmt.executeQuery();
            List<AccInfoEntry> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new AccInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccInfoEntry> getEntriesByPrefix(String group, String prefix) {
        String sql = "SELECT key, value FROM acc_info WHERE group_name = ? AND key LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group);
            stmt.setString(2, prefix + "%");
            ResultSet rs = stmt.executeQuery();
            List<AccInfoEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AccInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccInfoEntry> getEntriesByPrefixWithDepth(String group, String prefix, int depth) {
        String sql = """
            SELECT key, value FROM acc_info
            WHERE group_name = ?
              AND key LIKE ?
              AND (LENGTH(key) - LENGTH(REPLACE(key, '/', ''))) = ?;
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, group);
            stmt.setString(2, prefix + "/%");
            stmt.setInt(3, depth);
            ResultSet rs = stmt.executeQuery();
            List<AccInfoEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AccInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertEntries(String group, Map<String, String> files, List<String> folders) {
        String sql = "INSERT INTO acc_info (group_name, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                stmt.setString(1, group);
                stmt.setString(2, entry.getKey());
                stmt.setString(3, entry.getValue());
                stmt.addBatch();
            }
            for (String folder : folders) {
                stmt.setString(1, group);
                stmt.setString(2, folder);
                stmt.setString(3, "folder");
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getRegisteredAccGroupNames() {
        String sql = "SELECT DISTINCT group_name FROM acc_info ORDER BY group_name";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            List<String> groups = new ArrayList<>();
            while (rs.next()) {
                groups.add(rs.getString("group_name"));
            }
            return groups;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String[] getDefSQL() {
        return new String[]{
            """
            CREATE TABLE IF NOT EXISTS acc_info (
                group_name TEXT NOT NULL,
                key TEXT NOT NULL,
                value TEXT,
                FOREIGN KEY (group_name) REFERENCES acc_group(group_name)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE,
                UNIQUE (group_name, key)
            );
            """
        };
    }
}

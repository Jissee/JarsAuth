package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.model.AccFileInfoEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AccFileInfoDAO implements DAO {
    private final ConnectionProvider provider;

    public AccFileInfoDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }

    public List<String> getAllFileNames(String groupName){
        String sql = "SELECT key FROM acc_file_info WHERE group_name = ? ORDER BY `key`";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("key"));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccFileInfoEntry> getAllEntries(String groupName) {
        String sql = "SELECT key, value FROM acc_file_info WHERE group_name = ? ORDER BY `key`";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            List<AccFileInfoEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AccFileInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccFileInfoEntry> getEntriesByKey(String group, String key) {
        String sql = "SELECT key, value FROM acc_file_info WHERE group_name = ? AND key = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, group);
            stmt.setString(2, key);
            ResultSet rs = stmt.executeQuery();
            List<AccFileInfoEntry> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new AccFileInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccFileInfoEntry> getEntriesByPrefix(String group, String prefix) {
        String sql = "SELECT key, value FROM acc_file_info WHERE group_name = ? AND key LIKE ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, group);
            stmt.setString(2, prefix + "%");
            ResultSet rs = stmt.executeQuery();
            List<AccFileInfoEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AccFileInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccFileInfoEntry> getEntriesByPrefixWithDepth(String group, String prefix, int depth) {
        String sql = """
            SELECT key, value FROM acc_file_info
            WHERE group_name = ?
              AND key LIKE ?
              AND (LENGTH(key) - LENGTH(REPLACE(key, '/', ''))) = ?;
            """;
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, group);
            stmt.setString(2, prefix + "/%");
            stmt.setInt(3, depth);
            ResultSet rs = stmt.executeQuery();
            List<AccFileInfoEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new AccFileInfoEntry(rs.getString("key"), rs.getString("value")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertEntries(String group, Map<String, String> files, List<String> folders) {
        String sql = "INSERT INTO acc_file_info (group_name, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
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
        String sql = "SELECT DISTINCT group_name FROM acc_file_info ORDER BY group_name";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
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
    public Connection getConnection() throws SQLException {
        return provider.getConnection();
    }

    @Override
    public String[] getDefSQL() {
        return new String[]{
            """
            CREATE TABLE IF NOT EXISTS acc_file_info (
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

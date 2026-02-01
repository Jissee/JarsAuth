package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class LicenseGroupDAO implements DAO {
    private final ConnectionProvider provider;

    public LicenseGroupDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }
    public Set<String> getAllGroupNames() {
        String sql = "SELECT group_name FROM license_group ORDER BY group_name;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            Set<String> groups = new HashSet<>();
            while (rs.next()) {
                groups.add(rs.getString("group_name"));
            }
            return groups;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertGroup(String name) {
        String sql = "INSERT INTO license_group (group_name) VALUES (?);";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteGroup(String name) {
        String sql = "DELETE FROM license_group WHERE group_name = ?;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void renameGroup(String oldName, String newName) {
        String sql = "UPDATE license_group SET group_name = ? WHERE group_name = ?;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, newName);
            statement.setString(2, oldName);
            statement.executeUpdate();
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
        CREATE TABLE IF NOT EXISTS license_group (
            group_name TEXT PRIMARY KEY
        );
        """
        };
    }
}

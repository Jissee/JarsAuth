package me.jissee.jarsauth.data.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccGroupDAO implements DAO {
    private final Connection connection;

    public AccGroupDAO(Connection connection) {
        this.connection = connection;
        initTable();
    }

    public List<String> getAllGroupNames() {
        String sql = "SELECT group_name FROM acc_group ORDER BY group_name";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<String> groups = new ArrayList<>();
            while (rs.next()) {
                groups.add(rs.getString("group_name"));
            }
            return groups;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertGroup(String name) {
        String sql = "INSERT INTO acc_group (group_name) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteGroup(String name) {
        String sql = "DELETE FROM acc_group WHERE group_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void renameGroup(String oldName, String newName) {
        String sql = "UPDATE acc_group SET group_name = ? WHERE group_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, oldName);
            stmt.executeUpdate();
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
            CREATE TABLE IF NOT EXISTS acc_group (
                group_name TEXT PRIMARY KEY
            );
            """
        };
    }
}

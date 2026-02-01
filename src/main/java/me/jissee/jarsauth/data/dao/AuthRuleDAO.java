package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuthRuleDAO implements DAO {
    private final ConnectionProvider provider;

    public AuthRuleDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }

    public List<String> findRulesByGroup(String groupName) {
        String sql = "SELECT auth_rule FROM auth_profile WHERE group_name = ? ORDER BY auth_rule";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, groupName);
            ResultSet result = statement.executeQuery();
            List<String> rules = new ArrayList<>();
            while (result.next()) {
                rules.add(result.getString("auth_rule"));
            }
            return rules;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAllGroups() {
        String sql = "SELECT DISTINCT(group_name) FROM auth_profile ORDER BY group_name";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            ResultSet result = statement.executeQuery();
            List<String> rules = new ArrayList<>();
            while (result.next()) {
                rules.add(result.getString("group_name"));
            }
            return rules;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertRules(String groupName, List<String> rules) {
        String sql = "INSERT OR IGNORE INTO auth_profile (group_name, auth_rule) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            for (String rule : rules) {
                statement.setString(1, groupName);
                statement.setString(2, rule);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeRule(String groupName, String oldRule, String newRule) {
        String sql = "UPDATE auth_profile SET auth_rule = ? WHERE group_name = ? AND auth_rule = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, newRule);
            statement.setString(2, groupName);
            statement.setString(3, oldRule);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeRule(String groupName, String rule) {
        String sql = "DELETE FROM auth_profile WHERE group_name = ? AND auth_rule = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, groupName);
            statement.setString(2, rule);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeGroup(String groupName) {
        String sql = "DELETE FROM auth_profile WHERE group_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, groupName);
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
            CREATE TABLE IF NOT EXISTS auth_profile (
                group_name TEXT,
                auth_rule TEXT,
                FOREIGN KEY (group_name) REFERENCES acc_group(group_name)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE,
               UNIQUE(group_name, auth_rule)
            );
            """
        };
    }
}

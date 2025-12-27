package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserIdDAO implements DAO{
    private final ConnectionProvider provider;

    public UserIdDAO(ConnectionProvider provider) {
        this.provider = provider;
    }

    public Optional<UUID> getUserId(String userName){
        String sql = "SELECT user_id FROM user_id WHERE user_name = ?;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(UUID.fromString(resultSet.getString("user_id")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID saveUserId(String userName, UUID userId) {
        String sql = "INSERT INTO user_id(user_name, user_id) VALUES (?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setString(2, userId.toString());
            statement.executeUpdate();
            return userId;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeUserId(String userName) {
        String sql = "DELETE FROM user_id WHERE user_name = ?;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.executeUpdate();
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, UUID> getUserIds() {
        Map<String, UUID> result = new TreeMap<>();
        String sql = "SELECT user_name, user_id FROM user_id ORDER BY user_name;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.execute();
            try(ResultSet resultSet = statement.getResultSet()){
                while (resultSet.next()) {
                    result.put(resultSet.getString("user_name"), UUID.fromString(resultSet.getString("user_id")));
                }
                return result;
            }

        }catch (SQLException e){
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
        CREATE TABLE IF NOT EXISTS user_id(
            user_name TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            UNIQUE (user_name, user_id)
        );
        """
        };
    }
}

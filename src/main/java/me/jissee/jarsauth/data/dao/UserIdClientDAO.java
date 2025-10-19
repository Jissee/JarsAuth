package me.jissee.jarsauth.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UserIdClientDAO implements DAO{
    private final Connection connection;

    public UserIdClientDAO(Connection connection) {
        this.connection = connection;
    }

    public Optional<UUID> getUserId(String userName, UUID serverId) {
        String sql = "SELECT user_id FROM user_id_client WHERE user_name = ? AND server_id = ?;";
        try(PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, userName);
            statement.setString(2, serverId.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(UUID.fromString(resultSet.getString("user_id")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveUserId(String userName, UUID userId, UUID serverId) {
        String sql = "INSERT INTO user_id_client(user_name, user_id, server_id) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT(user_name, server_id) DO UPDATE SET user_id = excluded.user_id;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setString(2, userId.toString());
            statement.setString(3, serverId.toString());
            statement.executeUpdate();
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
        CREATE TABLE IF NOT EXISTS user_id_client(
            user_name TEXT NOT NULL,
            user_id TEXT NOT NULL,
            server_id TEXT,
            PRIMARY KEY(user_name, server_id)
        );
        """
        };
    }
}

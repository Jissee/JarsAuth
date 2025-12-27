package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class ServerIdDAO implements DAO{
    private final ConnectionProvider provider;

    public ServerIdDAO(ConnectionProvider provider) {
        this.provider = provider;
    }

    public Optional<UUID> getServerId() {
        String sql = "SELECT value FROM server_id;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if(result.next()) {
                String value = result.getString("value");
                return Optional.of(UUID.fromString(value));
            }
            return Optional.empty();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public UUID saveServerId(UUID serverId) {
        String sql = "INSERT INTO server_id VALUES (?);";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, serverId.toString());
            statement.execute();
            return serverId;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void resetServerId() {
        String sql = "DELETE FROM server_id;";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.execute();
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
        CREATE TABLE IF NOT EXISTS server_id (
            value TEXT PRIMARY KEY
        );
        """
        };
    }
}

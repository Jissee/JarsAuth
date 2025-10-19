package me.jissee.jarsauth.data.dao;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class ServerIdDAO implements DAO{
    private final Connection connection;

    public ServerIdDAO(Connection connection) {
        this.connection = connection;
    }

    public Optional<UUID> getServerId() {
        String sql = "SELECT value FROM server_id;";
        try(Statement statement = connection.createStatement()){
            ResultSet result = statement.executeQuery(sql);
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
        try(PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, serverId.toString());
            statement.execute();
            return serverId;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void resetServerId() {
        String sql = "DELETE FROM server_id;";
        try(PreparedStatement statement = connection.prepareStatement(sql)){
            statement.execute();
        }catch (SQLException e){
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
        CREATE TABLE IF NOT EXISTS server_id (
            value TEXT PRIMARY KEY
        );
        """
        };
    }
}

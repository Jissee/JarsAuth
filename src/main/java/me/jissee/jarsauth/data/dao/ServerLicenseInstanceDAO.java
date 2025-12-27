package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.model.ServerLicense;
import me.jissee.jarsauth.data.model.ServerLicenseInstance;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServerLicenseInstanceDAO implements DAO{
    private final ConnectionProvider provider;

    public ServerLicenseInstanceDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }

    public List<ServerLicenseInstance> getLicenseInstancesForGroup0(String groupName) {
        String sql =
                 """
                 SELECT player, license_id, group_name, group_chain, remaining
                 FROM server_license_instance
                 WHERE group_name = ?;
                 """;
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, groupName);
            ResultSet result = statement.executeQuery();
            List<ServerLicenseInstance> instances = new ArrayList<>();
            while (result.next()) {
                ServerLicenseInstance instance =
                        new ServerLicenseInstance(
                                result.getString("license_id"),
                                result.getString("player"),
                                result.getString("group_name"),
                                result.getString("group_chain"),
                                result.getLong("remaining")
                        );
                instances.add(instance);
            }
            return instances;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ServerLicenseInstance> getLicenseInstancesForPlayer(String playerName){
        String sql =
                """
                SELECT player, license_id, group_name, group_chain, remaining
                FROM server_license_instance
                WHERE player = ?;
                """;
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerName);
            ResultSet result = statement.executeQuery();
            List<ServerLicenseInstance> instances = new ArrayList<>();
            while (result.next()) {
                ServerLicenseInstance instance =
                        new ServerLicenseInstance(
                                result.getString("license_id"),
                                result.getString("player"),
                                result.getString("group_name"),
                                result.getString("group_chain"),
                                result.getLong("remaining")
                        );
                instances.add(instance);
            }
            return instances;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeLicenseInstance(ServerLicenseInstance instance) {
        removeLicenseInstance(instance.player(), instance.licenseId(), instance.groupName(), instance.groupChain());
    }

    public void removeLicenseInstance(String player, String license, String groupName, String groupChain) {
        String sql = "DELETE FROM server_license_instance WHERE player = ? AND license_id = ? AND group_name = ? AND group_chain = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, player);
            statement.setString(2, license);
            statement.setString(3, groupName);
            statement.setString(4, groupChain);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertInstances(List<ServerLicenseInstance> licenseInstances) {
        String sql = """
        INSERT OR IGNORE INTO server_license_instance
        (player, license_id, group_name, group_chain, remaining)
        VALUES (?, ?, ?, ?, ?);
        """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            for (ServerLicenseInstance licenseInstance : licenseInstances) {
                statement.setString(1, licenseInstance.player());
                statement.setString(2, licenseInstance.licenseId());
                statement.setString(3, licenseInstance.groupName());
                statement.setString(4, licenseInstance.groupChain());
                statement.setLong(5, licenseInstance.remaining());
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateInstances(List<ServerLicenseInstance> licenseInstances) {
        String sql = """
        INSERT INTO server_license_instance
        (player, license_id, group_name, group_chain, remaining)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(player, license_id, group_name, group_chain)
        DO UPDATE SET
            remaining = excluded.remaining;
        """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            for (ServerLicenseInstance licenseInstance : licenseInstances) {
                statement.setString(1, licenseInstance.player());
                statement.setString(2, licenseInstance.licenseId());
                statement.setString(3, licenseInstance.groupName());
                statement.setString(4, licenseInstance.groupChain());
                statement.setLong(5, licenseInstance.remaining());
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveInstances(List<ServerLicenseInstance> licenseInstances, boolean replaceExist) {
        if (licenseInstances == null || licenseInstances.isEmpty()) {
            return;
        }

        if (replaceExist) {
            updateInstances(licenseInstances);
        } else {
            insertInstances(licenseInstances);
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
        CREATE TABLE IF NOT EXISTS server_license_instance (
            player TEXT NOT NULL,
            license_id TEXT NOT NULL,
            group_name TEXT NOT NULL,
            group_chain TEXT NOT NULL,
            remaining LONG NOT NULL,
            PRIMARY KEY (player, license_id, group_name, group_chain)
        );
        """
        };
    }
}

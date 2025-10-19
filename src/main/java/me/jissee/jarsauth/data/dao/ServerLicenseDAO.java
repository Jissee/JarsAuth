package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.model.LicenseType;
import me.jissee.jarsauth.data.model.ServerLicense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServerLicenseDAO implements DAO{
    private final Connection connection;
    public ServerLicenseDAO(Connection connection) {
        this.connection = connection;
    }

    public List<ServerLicense> getAllLicenses() {
        String sql = "SELECT uuid, user_name, valid_from, valid_until, type, allowance, period FROM server_license ORDER BY user_name, valid_until; ";
        try(PreparedStatement statement = connection.prepareStatement(sql)){
            List<ServerLicense> licenses = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()){
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String userName = resultSet.getString("user_name");
                long validFrom = resultSet.getLong("valid_from");
                long validUntil = resultSet.getLong("valid_until");
                LicenseType type = LicenseType.fromCode(resultSet.getInt("type"));
                long allowance = resultSet.getLong("allowance");
                long allowancePeriod = resultSet.getLong("period");
                ServerLicense license = new ServerLicense(uuid, userName, validFrom, validUntil, type, allowance, allowancePeriod);
                licenses.add(license);
            }
            return licenses;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public List<ServerLicense> getLicensesForUser(String userName){
        String sql = "SELECT uuid, user_name, valid_from, valid_until, type, allowance, period FROM server_license WHERE user_name = ? ORDER BY valid_until; ";
        try(PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, userName);
            List<ServerLicense> licenses = new ArrayList<>();
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()){
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                long validFrom = resultSet.getLong("valid_from");
                long validUntil = resultSet.getLong("valid_until");
                LicenseType type = LicenseType.fromCode(resultSet.getInt("type"));
                long allowance = resultSet.getLong("allowance");
                long allowancePeriod = resultSet.getLong("period");
                ServerLicense license = new ServerLicense(
                        uuid,
                        userName,
                        validFrom,
                        validUntil,
                        type,
                        allowance,
                        allowancePeriod
                );
                licenses.add(license);
            }
            return licenses;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public ServerLicense getLicense(UUID uuid) {
        String sql = "SELECT uuid, user_name, valid_from, valid_until, type, allowance, period FROM server_license WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new ServerLicense(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("user_name"),
                        rs.getLong("valid_from"),
                        rs.getLong("valid_until"),
                        LicenseType.fromCode(rs.getInt("type")),
                        rs.getLong("allowance"),
                        rs.getLong("period")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAllowance(UUID uuid, int delta) {
        String sql = "UPDATE server_license SET allowance = allowance + ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, delta);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAllowance(UUID uuid, int allowance) {
        String sql = "UPDATE server_license SET allowance = ? WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowance);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeLicense(UUID uuid) {
        String sql = "DELETE FROM server_license WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeExpiredLicenses() {
        String sql = "DELETE FROM server_license WHERE valid_until < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, System.currentTimeMillis() / 1000);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void saveLicense(ServerLicense license){
        String sql = "INSERT INTO server_license(uuid, user_name, valid_from, valid_until, type, allowance, period) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try(PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, license.uuid().toString());
            statement.setString(2, license.userName());
            statement.setLong(3, license.validFrom());
            statement.setLong(4, license.validUntil());
            statement.setInt(5, license.type().getCode());
            statement.setLong(6, license.allowance().get());
            statement.setLong(7, license.period().get());
            statement.execute();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void updateLicense(ServerLicense license) {
        String sql = """
        UPDATE server_license
        SET user_name = ?,
            valid_from = ?,
            valid_until = ?,
            type = ?,
            allowance = ?,
            period = ?
        WHERE uuid = ?
    """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, license.userName());
            statement.setLong(2, license.validFrom());
            statement.setLong(3, license.validUntil());
            statement.setInt(4, license.type().getCode());
            statement.setLong(5, license.allowance().get());
            statement.setLong(6, license.period().get());
            statement.setString(7, license.uuid().toString());
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
        CREATE TABLE IF NOT EXISTS `server_license` (
            uuid TEXT PRIMARY KEY,
            user_name TEXT NOT NULL,
            valid_from DATETIME NOT NULL,
            valid_until DATETIME NOT NULL,
            type INTEGER NOT NULL,
            allowance LONG NOT NULL,
            period LONG NOT NULL
        );
        """
        };
    }
}

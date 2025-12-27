package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.model.ServerLicense;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ServerLicenseDAO implements DAO{
    private final ConnectionProvider provider;
    public ServerLicenseDAO(ConnectionProvider provider) {
        this.provider = provider;
    }

    public String getNextAvailableId() {
        String sql =
                "SELECT IFNULL((" +
                        "   SELECT id + 1 " +
                        "   FROM server_license " +
                        "   WHERE NOT EXISTS (" +
                        "       SELECT 1 FROM server_license s2 " +
                        "       WHERE s2.id = server_license.id + 1" +
                        "   ) " +
                        "   ORDER BY id " +
                        "   LIMIT 1" +
                        "), 0)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return String.valueOf(rs.getInt(1));
            }
            throw new RuntimeException("Failed to get next available id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists(String id) {
        String sql = "SELECT COUNT(id) FROM server_license where id = ?; ";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            try(ResultSet resultSet = statement.executeQuery()){
                if(resultSet.next()) {
                    return resultSet.getInt(1) >= 1;
                }
                throw new RuntimeException("Failed to get id from server_license");
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public List<ServerLicense> getAllLicenses() {
        String sql = "SELECT id, valid_from, valid_until, type, reset_time, clear_time, allowance FROM server_license ORDER BY id; ";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            List<ServerLicense> licenses = new ArrayList<>();
            try(ResultSet resultSet = statement.executeQuery()){
                while(resultSet.next()){
                    String id = resultSet.getString("id");
                    LocalDate validFrom = resultSet.getDate("valid_from").toLocalDate();
                    LocalDate validUntil = resultSet.getDate("valid_until").toLocalDate();
                    int type = resultSet.getInt("type");
                    LocalTime resetTime = resultSet.getTime("reset_time").toLocalTime();
                    LocalTime clearTime = resultSet.getTime("clear_time").toLocalTime();
                    long allowance = resultSet.getLong("allowance");
                    ServerLicense license = new ServerLicense(id, validFrom, validUntil, type, resetTime, clearTime, allowance);
                    licenses.add(license);
                }
                return licenses;
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public ServerLicense getLicense(String id) {
        String sql = "SELECT id, valid_from, valid_until, type, reset_time, clear_time, allowance FROM server_license WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            try(ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new ServerLicense(
                            rs.getString("id"),
                            rs.getDate("valid_from").toLocalDate(),
                            rs.getDate("valid_until").toLocalDate(),
                            rs.getInt("type"),
                            rs.getTime("reset_time").toLocalTime(),
                            rs.getTime("clear_time").toLocalTime(),
                            rs.getLong("allowance")
                    );
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeLicense(String id) {
        String sql = "DELETE FROM server_license WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void saveLicense(ServerLicense license){
        String sql = "INSERT INTO server_license(id, valid_from, valid_until, type, reset_time, clear_time, allowance) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, license.id());
            statement.setDate(2, Date.valueOf(license.validFrom()));
            statement.setDate(3, Date.valueOf(license.validUntil()));
            statement.setInt(4, license.type());
            statement.setTime(5, Time.valueOf(license.resetTime()));
            statement.setTime(6, Time.valueOf(license.clearTime()));
            statement.setLong(7, license.allowance());
            statement.execute();
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public void updateLicense(String id, ServerLicense newLicense) {
        String sql = """
        UPDATE server_license
        SET id = ?,
            valid_from = ?,
            valid_until = ?,
            type = ?,
            reset_time = ?,
            clear_time = ?,
            allowance = ?
        WHERE id = ?
    """;
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, newLicense.id());
            statement.setDate(2, Date.valueOf(newLicense.validFrom()));
            statement.setDate(3, Date.valueOf(newLicense.validUntil()));
            statement.setInt(4, newLicense.type());
            statement.setTime(5, Time.valueOf(newLicense.resetTime()));
            statement.setTime(6, Time.valueOf(newLicense.clearTime()));
            statement.setLong(7, newLicense.allowance());
            statement.setString(8, id);
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
        CREATE TABLE IF NOT EXISTS `server_license` (
            id TEXT PRIMARY KEY,
            valid_from DATE NOT NULL,
            valid_until DATE NOT NULL,
            type INTEGER NOT NULL,
            reset_time TIME NOT NULL,
            clear_time TIME NOT NULL,
            allowance LONG NOT NULL
        );
        """
        };
    }


}

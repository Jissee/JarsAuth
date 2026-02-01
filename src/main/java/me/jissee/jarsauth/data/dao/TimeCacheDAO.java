package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.*;
import java.time.LocalDateTime;

public class TimeCacheDAO implements DAO {
    private final ConnectionProvider provider;

    public TimeCacheDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }

    public LocalDateTime get(String playerName) {
        String sql =
                """
                SELECT date, time
                FROM time_cache
                WHERE player_name = ?;
                """;
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerName);
            try(ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Date date = rs.getDate("date");
                    Time time = rs.getTime("time");
                    return LocalDateTime.of(date.toLocalDate(), time.toLocalTime());
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(String playerName, LocalDateTime dateTime) {
        String sql = """
        INSERT INTO time_cache
        (player_name, date, time)
        VALUES (?, ?, ?)
        ON CONFLICT(player_name)
        DO UPDATE SET
            date = excluded.date,
            time = excluded.time;
        """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setDate(2, Date.valueOf(dateTime.toLocalDate()));
            statement.setTime(3, Time.valueOf(dateTime.toLocalTime()));
            statement.execute();
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
            CREATE TABLE IF NOT EXISTS time_cache (
                player_name TEXT PRIMARY KEY,
                date DATE NOT NULL,
                time TIME NOT NULL
            );
            """
        };
    }


}

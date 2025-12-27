package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.model.ServerLicenseInstance;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimeCacheDAO implements DAO{
    private final ConnectionProvider provider;

    public TimeCacheDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }

    public Optional<LocalDateTime> get(String playerName, String tag) {
        String sql =
                """
                SELECT player, tag, date, time
                FROM time_cache
                WHERE player = ? AND tag = ?;
                """;
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setString(2, tag);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                LocalDateTime dateTime =
                        LocalDateTime.of(
                                result.getDate("date").toLocalDate(),
                                result.getTime("time").toLocalTime()
                        );
                return Optional.of(dateTime);
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(String playerName, String tag, LocalDateTime dateTime) {
        String sql = """
        INSERT INTO time_cache
        (player, tag, date, time)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(player, tag)
        DO UPDATE SET
            time = excluded.time;
        """;

        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerName);
            statement.setString(2, tag);
            statement.setDate(3, Date.valueOf(dateTime.toLocalDate()));
            statement.setTime(4, Time.valueOf(dateTime.toLocalTime()));
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
                    player TEXT NOT NULL,
                    tag TEXT NOT NULL,
                    date DATE NOT NULL,
                    time TIME NOT NULL,
                    PRIMARY KEY (player, tag)
                );
                """
        };
    }


}

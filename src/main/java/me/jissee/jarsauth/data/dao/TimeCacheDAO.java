package me.jissee.jarsauth.data.dao;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class TimeCacheDAO implements DAO {
    private final ConnectionProvider provider;

    public TimeCacheDAO(ConnectionProvider provider) {
        this.provider = provider;
        initTable();
    }
    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public String[] getDefSQL() {
        return new String[0];
    }
}

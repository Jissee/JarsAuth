package me.jissee.jarsauth.data.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface DAO {
    default void initTable(){
        for(String def : getDefSQL()){
            try(Connection connection = getConnection()) {
                connection.prepareStatement(def).execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    Connection getConnection() throws SQLException;

    String[] getDefSQL();
}

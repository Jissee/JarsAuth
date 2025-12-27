package me.jissee.jarsauth.data.dao;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

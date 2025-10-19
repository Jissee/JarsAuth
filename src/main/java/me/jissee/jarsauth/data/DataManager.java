package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.service.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DataManager {
    private static final String DB_NAME = "jarsauth.db";
    private static DataManager clientInstance;
    private static DataManager serverInstance;

    private Map<Class<? extends Service>, Service> services = new HashMap<>();
    private Connection connection;

    public static DataManager getClientInstance() {
        if (clientInstance == null) clientInstance = new DataManager(true, false);
        return clientInstance;
    }

    public static DataManager getServerInstance() {
        if (serverInstance == null) serverInstance = new DataManager(false, true);
        return serverInstance;
    }

    public DataManager(boolean isTemp, boolean isServer) {
        initConnection(isTemp);
        initForeignKey();
        if (isServer) {
            registerServer();
        }else{
            registerClient();
        }
    }

    private void registerServer(){
        registerService(ConfigService.class, ConfigService::new);
        registerService(AcceptedDetailService.class, AcceptedDetailService::new);
        registerService(AuthProfileService.class, AuthProfileService::new);
        registerService(ServerIdService.class, ServerIdService::new);
        registerService(UserIdService.class, UserIdService::new);
        registerService(ServerLicenseService.class, ServerLicenseService::new);
    }

    private void registerClient(){
        registerService(UserIdClientService.class, UserIdClientService::new);
    }

    public Connection getConnection() {
        return connection;
    }

    private void initConnection(boolean isTemp) {
        if(connection != null){
            return;
        }
        try {
            if (isTemp) {
                connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            }
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private void initForeignKey() {
        try(Statement statement = connection.createStatement()){
            statement.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Service> void registerService(Class<T> clazz, Function<Connection, T> serviceProvider) {
        Service service = serviceProvider.apply(connection);
        service.initTable();
        services.put(clazz, service);
    }

    public <T extends Service> T getService(Class<T> clazz) {
        return (T) services.get(clazz);
    }


}

package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.service.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DataManager {
    private static final String DB_NAME = "jarsauth.db";
    private static final String DB_MEMORY = ":memory:";
    private static final String DB_URL_PREFIX = "jdbc:sqlite:";
    private static DataManager clientInstance;
    private static DataManager serverInstance;

    private final String dbName;

    private Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();

    public static DataManager getClientInstance() {
        if (clientInstance == null) clientInstance = new DataManager(DB_NAME, false);
        return clientInstance;
    }

    public static DataManager getServerInstance() {
        if (serverInstance == null) serverInstance = new DataManager(DB_NAME, true);
        return serverInstance;
    }

    public DataManager(String dbName, boolean isServer) {
        this.dbName = dbName;
        try {
            initProperty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (isServer) {
            registerServer();
        }else{
            registerClient();
        }
    }

    private void registerServer(){
        registerService(ConfigService.class, ConfigService::new);
        registerService(AccProfileService.class, AccProfileService::new);
        registerService(AuthRuleService.class, AuthRuleService::new);
        registerService(ServerIdService.class, ServerIdService::new);
        registerService(UserIdServerService.class, UserIdServerService::new);
        registerService(ServerLicenseService.class, ServerLicenseService::new);
        registerService(LicenseGroupRuleService.class, LicenseGroupRuleService::new);
        registerService(LicenseGroupService.class, LicenseGroupService::new);
        registerService(TimeCacheService.class, TimeCacheService::new);

        for(Map.Entry<Class<? extends Service>, Service> entry : services.entrySet()){
            Service service = entry.getValue();
            service.inject(this::getService);
        }
    }

    private void registerClient(){
        registerService(ClientDataService.class, ClientDataService::new);
    }

    private String getConnectionStr(){
        return DB_URL_PREFIX + dbName;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(getConnectionStr());
        try(Statement statement = connection.createStatement()){
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute("PRAGMA busy_timeout = 5000;");
        }
        return connection;
    }


    private void initProperty() throws SQLException{
        Connection connection = DriverManager.getConnection(getConnectionStr());
        try(Statement statement = connection.createStatement()){
            statement.execute("PRAGMA journal_mode = WAL;");
            statement.execute("PRAGMA synchronous = NORMAL;");
        }
    }

    private <T extends Service> void registerService(Class<T> clazz, Function<ConnectionProvider, T> serviceProvider) {
        Service service = serviceProvider.apply(this::getConnection);
        service.initTable();
        services.put(clazz, service);
    }



    public <T extends Service> T getService(Class<T> clazz) {
        return (T) services.get(clazz);
    }


}

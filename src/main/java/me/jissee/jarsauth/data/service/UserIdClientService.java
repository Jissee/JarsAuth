package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.dao.UserIdClientDAO;

import java.sql.Connection;
import java.util.Optional;
import java.util.UUID;

public class UserIdClientService implements Service {
    private final UserIdClientDAO dao;
    public UserIdClientService(Connection connection) {
        this.dao = new UserIdClientDAO(connection);
    }

    public Optional<UUID> getUserId(String userName, UUID serverId) {
        return dao.getUserId(userName, serverId);
    }

    public void saveUserId(String userName, UUID userId, UUID serverId) {
        dao.saveUserId(userName, userId, serverId);
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}

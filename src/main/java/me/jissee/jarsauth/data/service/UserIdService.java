package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.UserIdDAO;

import java.sql.Connection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserIdService implements Service {
    private final UserIdDAO dao;

    public UserIdService(ConnectionProvider provider) {
        this.dao = new UserIdDAO(provider);
    }

    public boolean hasUserId(String userName){
        Optional<UUID> got = dao.getUserId(userName);
        return got.isPresent();
    }

    public UUID getOrCreateUserId(String userName) {
        Optional<UUID> got = dao.getUserId(userName);
        return got.orElseGet(() -> dao.saveUserId(userName, UUID.randomUUID()));
    }

    public Map<String, UUID> getUserIds() {
        return dao.getUserIds();
    }

    public void removeUserId(String userName) {
        dao.removeUserId(userName);
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}

package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.UserIdServerDAO;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserIdServerService implements Service {
    private final UserIdServerDAO dao;

    public UserIdServerService(ConnectionProvider provider) {
        this.dao = new UserIdServerDAO(provider);
    }

    public boolean hasUserId(String userName){
        Optional<UUID> got = dao.getUserId(userName);
        return got.isPresent();
    }

    public Optional<UUID> getOrCreateUserId(String userName) {
        Optional<UUID> got = dao.getUserId(userName);
        if (got.isPresent()) {
            return got;
        }else {
            return Optional.of(dao.saveUserId(userName, UUID.randomUUID()));
        }
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

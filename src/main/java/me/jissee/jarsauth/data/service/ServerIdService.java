package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.ServerIdDAO;

import java.util.Optional;
import java.util.UUID;

public class ServerIdService implements Service {
    private ServerIdDAO dao;

    public ServerIdService(ConnectionProvider provider) {
        this.dao = new ServerIdDAO(provider);
    }

    public UUID getOrCreateServerId() {
        Optional<UUID> serverId = dao.getServerId();
        return serverId.orElseGet(() -> dao.saveServerId(UUID.randomUUID()));
    }

    public void resetServerId() {
        dao.resetServerId();
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}

package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.dao.ServerIdDAO;
import me.jissee.jarsauth.data.model.AcceptedDetail;

import java.sql.Connection;
import java.util.Optional;
import java.util.UUID;

public class ServerIdService implements Service {
    private ServerIdDAO dao;

    public ServerIdService(Connection connection) {
        this.dao = new ServerIdDAO(connection);
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

package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.TimeCacheDAO;

import java.time.LocalDateTime;
import java.util.Optional;

public class TimeCacheService implements Service {
    private final TimeCacheDAO dao;
    public TimeCacheService(ConnectionProvider provider) {
        this.dao = new TimeCacheDAO(provider);
    }
    public Optional<LocalDateTime> get(String playerName, String tag){
        return dao.get(playerName, tag);
    }

    public void set(String playerName, String tag, LocalDateTime dateTime){
        dao.set(playerName, tag, dateTime);
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}

package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.TimeCacheDAO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeCacheService implements Service {
    private final Map<String,LocalDateTime> cache;
    private final TimeCacheDAO dao;
    public TimeCacheService(ConnectionProvider provider) {
        cache = new HashMap<>();
        dao = new TimeCacheDAO(provider);
    }

    // used for time elapsed when logged in
    public LocalDateTime getVolatile(String playerName){
        return cache.get(playerName);
    }

    public void setVolatile(String playerName, LocalDateTime dateTime){
        cache.put(playerName, dateTime);
    }

    // used for update and reset allowance
    public LocalDateTime getPermanent(String playerName){
        return dao.get(playerName);
    }

    public void setPermanent(String playerName, LocalDateTime dateTime){
        dao.set(playerName, dateTime);
    }

    @Override
    public void initTable() {
        dao.initTable();
    }
}

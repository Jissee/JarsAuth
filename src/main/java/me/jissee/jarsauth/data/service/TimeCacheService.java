package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.data.ConnectionProvider;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeCacheService implements Service {
    private final Map<String,LocalDateTime> cache;
    public TimeCacheService(ConnectionProvider provider) {
        cache = new HashMap<>();
    }
    public LocalDateTime getVolatile(String playerName){
        return cache.get(playerName);
    }

    public void setVolatile(String playerName, LocalDateTime dateTime){
        cache.put(playerName, dateTime);
    }

    @Override
    public void initTable() {

    }
}

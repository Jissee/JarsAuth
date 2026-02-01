package me.jissee.jarsauth.data.service;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.ConnectionProvider;
import me.jissee.jarsauth.data.dao.ConfigDAO;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigService implements Service {

    private final ConfigDAO dao;

    public ConfigService(ConnectionProvider provider) {
        this.dao = new ConfigDAO(provider);
    }

    @Override
    public void initTable() {
        dao.initTable();
        dao.initValues();
    }

    public long getValue(ConfigKey key) {
        long value = dao.getValue(key);
        if (value < 0) {
            LoggerFactory.getLogger("JarsAuth").warn("Value for key {} is negative, change to default value {}.", key.getKey(), key.getDefaultValue());
            dao.setValue(key, key.getDefaultValue());
            return key.getDefaultValue();
        }
        return value;
    }

    public void setValue(ConfigKey key, long value) {
        dao.setValue(key, value);
    }

    public void setCheckedValue(ConfigKey key, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Config value must be non-negative: " + key.getKey());
        }
        dao.setValue(key, value);
    }

    /**
     * 返回所有配置项及其值，按 ConfigKey.ordinal 顺序
     */
    public Map<ConfigKey, Long> listAllValues() {
        Map<String, Long> rawMap = dao.listAllValues();
        Map<ConfigKey, Long> result = new LinkedHashMap<>();
        for (ConfigKey key : ConfigKey.values()) {
            Long val = rawMap.getOrDefault(key.getKey(), key.getDefaultValue());
            result.put(key, val);
        }
        return result;
    }
}

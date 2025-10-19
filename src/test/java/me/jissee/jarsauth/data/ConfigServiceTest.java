package me.jissee.jarsauth.data;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.service.ConfigService;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigServiceTest {

    private DataManager dataManager;
    private ConfigService configService;

    @BeforeEach
    public void setup() {
        dataManager = new DataManager(false, true); // 使用内存数据库并注册服务
        configService = dataManager.getService(ConfigService.class);
    }

    @Test
    public void testInitValueAndGet() {
        for (ConfigKey key : ConfigKey.values()) {
            long value = configService.getValue(key);
            assertEquals(key.getDefaultValue(), value,
                    "Initial value for " + key.getKey() + " should match default");
        }
    }

    @Test
    public void testSetAndGetValue() {
        ConfigKey key = ConfigKey.FILE_CHECKSUM_INTERVAL;
        configService.setValue(key, 123);
        long value = configService.getValue(key);
        assertEquals(123, value, "Set/Get value mismatch for key: " + key.getKey());
    }

    @Test
    public void testSetCheckedValueRejectsNegative() {
        ConfigKey key = ConfigKey.CLIENT_AUTH_TIMEOUT;
        assertThrows(IllegalArgumentException.class, () -> {
            configService.setCheckedValue(key, -5);
        }, "Negative value should be rejected by setCheckedValue");
    }

    @Test
    public void testNegativeValueAutoCorrected() {
        ConfigKey key = ConfigKey.CLIENT_AUTH_INTERVAL;
        configService.setValue(key, -10);
        long value = configService.getValue(key);
        assertEquals(key.getDefaultValue(), value,
                "Negative value should be corrected to default");
    }

    @Test
    public void testListAllValuesContainsAllKeys() {
        var allValues = configService.listAllValues();
        assertEquals(ConfigKey.values().length, allValues.size(), "All ConfigKey entries should be listed");
        for (ConfigKey key : ConfigKey.values()) {
            assertTrue(allValues.containsKey(key), "Missing key: " + key.getKey());
        }
    }
}

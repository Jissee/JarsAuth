package me.jissee.jarsauth.config;

import java.io.*;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static me.jissee.jarsauth.JarsAuth.MODID;

public class StaticConfig {
    private static final Logger logger = LoggerFactory.getLogger(MODID);
    private final Properties properties = new Properties();
    private static final File CONFIG_FILE = new File("static-config.properties");


    private static final StaticConfig instance = new StaticConfig();
    private StaticConfig() {
        setDefaults();

        if (CONFIG_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                Properties loaded = new Properties();
                loaded.load(fis);
                for (ConfigKey key : ConfigKey.values()) {
                    String loadedValue = loaded.getProperty(key.getKey());
                    if (loadedValue != null && isValid(key, loadedValue)) {
                        properties.setProperty(key.getKey(), loadedValue);
                    } else if (loadedValue == null) {
                        // missing, use default
                    } else {
                        logger.warn("Invalid value for key '{}': '{}', using default: '{}'",
                                key.getKey(), loadedValue, properties.getProperty(key.getKey()));
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to load config file: {}", e.toString());
            }
        }

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Static Configuration");
        } catch (IOException e) {
            logger.warn("Failed to save config file: {}", e.toString());
        }
    }

    private void setDefaults() {
        properties.setProperty(ConfigKey.FILE_CHECKSUM_ENABLED.getKey(), "false");
        properties.setProperty(ConfigKey.FILE_CHECKSUM_INTERVAL.getKey(), "20");
        properties.setProperty(ConfigKey.FILE_CHECKSUM_TIMEOUT.getKey(), "10");

        properties.setProperty(ConfigKey.CLIENT_AUTH_ENABLED.getKey(), "false");
        properties.setProperty(ConfigKey.CLIENT_AUTH_INTERVAL.getKey(), "20");
        properties.setProperty(ConfigKey.CLIENT_AUTH_TIMEOUT.getKey(), "10");

        properties.setProperty(ConfigKey.SERVER_LICENSE_ENABLED.getKey(), "false");
        properties.setProperty(ConfigKey.SERVER_LICENSE_AUTO_REMOVE.getKey(), "true");
        properties.setProperty(ConfigKey.SERVER_LICENSE_INTERVAL.getKey(), "10");
    }

    private boolean isValid(ConfigKey key, String value) {
        String name = key.name();
        if (name.endsWith("_ENABLED") || name.endsWith("_AUTO_REMOVE")) {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
        }
        if (name.endsWith("_INTERVAL") || name.endsWith("_TIMEOUT") ||
                name.endsWith("_AUTH_PER_MINUTE") || name.endsWith("_TRIAL_TIME")) {
            try {
                int val = Integer.parseInt(value);
                return val >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static StaticConfig getInstance() {
        return instance;
    }

    public String get(ConfigKey key) {
        return properties.getProperty(key.getKey());
    }

    public boolean getBoolean(ConfigKey key) {
        return Boolean.parseBoolean(get(key));
    }

    public int getInt(ConfigKey key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

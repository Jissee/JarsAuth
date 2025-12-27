package me.jissee.jarsauth.config;

public enum ConfigKey {
    FILE_CHECKSUM_ENABLED("file-checksum.enabled", 0),
    FILE_CHECKSUM_INTERVAL("file-checksum.interval", 20),
    FILE_CHECKSUM_TIMEOUT("file-checksum.timeout", 10),

    CLIENT_AUTH_ENABLED("client-auth.enabled", 0),
    CLIENT_AUTH_INTERVAL("client-auth.interval", 20),
    CLIENT_AUTH_TIMEOUT("client-auth.timeout", 10),

    SERVER_LICENSE_ENABLED("server-license.enabled", 0),
    SERVER_LICENSE_AUTO_REMOVE("server-license.auto-remove", 0),
    SERVER_LICENSE_INTERVAL("server-license.interval", 20),

    UI_LANGUAGE("ui.language", 0);


    private final String key;
    private final long defaultValue;

    ConfigKey(String key, long defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public long getDefaultValue() {
        return defaultValue;
    }

}

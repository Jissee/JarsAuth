package me.jissee.jarsauth.data.model;

public enum LicenseType {
    TIME(1,"config.sl.limit.time"),
    WALK(2, "config.sl.limit.walk"),
    DIG(3, "config.sl.limit.dig"),
    PICK(4, "config.sl.limit.pick"),
    UNLIMITED(114514, "config.sl.limit.unlimited");

    private final int code;
    private final String nameKey;

    LicenseType(int code, String nameKey) {
        this.code = code;
        this.nameKey = nameKey;
    }

    public int getCode() {
        return code;
    }

    public String getNameKey() {
        return nameKey;
    }

    // 根据 code 获取 enum
    public static LicenseType fromCode(int code) {
        for (LicenseType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("未知类型: " + code);
    }

    // 根据展示字符串获取 enum（可选）
    public static LicenseType fromDisplayName(String name) {
        for (LicenseType type : values()) {
            if (type.nameKey.equals(name)) return type;
        }
        throw new IllegalArgumentException("未知展示名称: " + name);
    }
}

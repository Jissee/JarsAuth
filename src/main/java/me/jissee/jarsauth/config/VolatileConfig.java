package me.jissee.jarsauth.config;

import java.util.concurrent.atomic.AtomicBoolean;

public class VolatileConfig {
    private static final VolatileConfig instance = new VolatileConfig();
    private VolatileConfig(){

    }

    private final AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode = new AtomicBoolean(false);
    public  AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode() {
        return ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode;
    }

    public static VolatileConfig getInstance() {
        return instance;
    }

}

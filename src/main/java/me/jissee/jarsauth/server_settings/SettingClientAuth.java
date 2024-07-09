/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingClientAuth {
    private static final String FILE_NAME = "setting-client-auth.json";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final boolean enabled;
    private final int interval;
    private final int timeout;

    public SettingClientAuth(boolean enabled, int interval, int timeout){
        this.enabled = enabled;
        this.interval = interval;
        this.timeout = timeout;
    }
    public static SettingClientAuth load(String serverSaveDir){
        File file = new File(serverSaveDir + FILE_NAME);

        if(!file.exists()){
            try {
                LOGGER.info("Cannot find " + FILE_NAME + ", generating default setting.");
                file.createNewFile();
                SettingClientAuth setting = createDefault();

                FileWriter fw = new FileWriter(file);
                fw.write(gson.toJson(setting));
                fw.close();

                return setting;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try(FileReader reader = new FileReader(file)){
            int ch;
            StringBuilder sb = new StringBuilder();
            while((ch = reader.read()) != -1){
                sb.append((char)ch);
            }
            String str = sb.toString();
            return gson.fromJson(str, SettingClientAuth.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static SettingClientAuth createDefault(){
        boolean enabled = false;
        int interval = 10;
        int timeout = 5;

        return new SettingClientAuth(enabled, interval, timeout);
    }

    public boolean isEnabled(){
        return enabled;
    }
    public int getInterval(){
        return interval;
    }
    public int getTimeout(){
        return timeout;
    }
}

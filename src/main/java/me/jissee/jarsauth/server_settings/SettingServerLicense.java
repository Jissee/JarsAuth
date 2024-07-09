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

public class SettingServerLicense {
    private static final String FILE_NAME = "setting-server-license.json";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final boolean enabled;
    private final boolean autoRemove;
    private final int authPerMinute;
    private final int generalAllowance;

    public SettingServerLicense(
            boolean enabled,
            boolean autoRemove,
            int authPerMinute,
            int generalAllowance
    ){
        this.enabled = enabled;
        this.autoRemove = autoRemove;
        this.authPerMinute = authPerMinute;
        this.generalAllowance = generalAllowance;
    }
    public static SettingServerLicense load(String serverSaveDir){
        File file = new File( serverSaveDir + FILE_NAME);

        if(!file.exists()){
            try{
                LOGGER.info("Cannot find " + FILE_NAME + ", generating default setting.");
                file.createNewFile();
                SettingServerLicense setting = createDefault();

                FileWriter fw = new FileWriter(file);
                fw.write(gson.toJson(setting));
                fw.close();

                return setting;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try(FileReader fr = new FileReader(file)) {
            int ch;
            StringBuilder sb = new StringBuilder();
            while((ch = fr.read()) != -1){
                sb.append((char)ch);
            }
            String str = sb.toString();
            return gson.fromJson(str, SettingServerLicense.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SettingServerLicense createDefault() {
        boolean enabled = false;
        boolean autoRemove = false;
        int authPerMinute = 4;
        int allowNoLicense = 0;

        return new SettingServerLicense(enabled, autoRemove, authPerMinute, allowNoLicense);
    }



    public boolean isEnabled() {
        return enabled;
    }
    public boolean isAutoRemove(){
        return autoRemove;
    }
    public int getAuthPerMinute() {
        return authPerMinute;
    }
    public int getGeneralAllowance() {
        return generalAllowance;
    }
}

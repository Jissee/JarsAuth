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
import java.util.ArrayList;

public class SettingFileChecksum {
    private static final String FILE_NAME = "setting-file-checksum.json";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final boolean enabled;
    private final int interval;
    private final int timeout;
    private final ArrayList<String> inclusion;

    public SettingFileChecksum(
            boolean enabled,
            int interval,
            int timeout,
            ArrayList<String> inclusion
    ){
        this.enabled = enabled;
        this.interval = interval;
        this.timeout = timeout;
        this.inclusion = inclusion;
    }
    public static SettingFileChecksum load(String serverSaveDir){
        File file = new File( serverSaveDir + FILE_NAME);

        if(!file.exists()){
            try{
                LOGGER.info("Cannot find " + FILE_NAME + ", generating default setting.");
                file.createNewFile();
                SettingFileChecksum setting = createDefault();

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
            return gson.fromJson(str, SettingFileChecksum.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static SettingFileChecksum createDefault() {
        boolean enabled = false;
        int interval = 20;
        int timeout = 10;
        ArrayList<String> inclusion = new ArrayList<>();
        inclusion.add("mods/*");
        inclusion.add("resourcepacks/examplefile.zip");
        inclusion.add("shaderpacks");
        inclusion.add("example.txt");

        return new SettingFileChecksum(enabled, interval, timeout, inclusion);
    }


    public boolean isEnabled(){
        return enabled;
    }
    public long getInterval() {
        return interval;
    }
    public long getTimeout(){
        return timeout;
    }
    public ArrayList<String> getInclusion(){
        return inclusion;
    }
}

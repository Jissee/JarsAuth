/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.event.EventHandler;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class ClientDetail {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, String> map = new HashMap<>();

    private static final ArrayList<Map<String,String>> allDetails = new ArrayList<>();

    public static void add(String key, String value){
        synchronized (map){
            if(key.equals("<end>") && value.equals("<end>")){
                Map<String, String> result = new LinkedHashMap<>();
                map.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered((entry) -> result.put(entry.getKey(), entry.getValue()));
                int i = 0;
                File file;
                String serverSaveDir = EventHandler.getServerSaveDir();
                do {
                    file = new File(serverSaveDir + "acc-" + i + ".json");
                    i++;
                } while (file.exists());
                try{
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try(FileOutputStream fos = new FileOutputStream(file)) {
                    try(OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)){
                        osw.write(gson.toJson(result));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                map.clear();
                EventHandler.reloadDetails();
            }else{
                map.put(key, value);
            }
        }
    }

    public static void reloadDetails(String serverSaveDir){
        Function<File, ArrayList<String>> getFilesInFolder = (dir) -> {
            ArrayList<String> result = new ArrayList<>();
            if (!dir.exists() || dir.isFile()) {
                return result;
            } else if (dir.isDirectory()) {
                for (File fl : dir.listFiles()) {
                    if (fl.isFile()) {
                        result.add(fl.getPath());
                    }
                }
            }
            return result;
        };
        ArrayList<String> files = getFilesInFolder.apply(new File(serverSaveDir));
        for(int i = 0; i < files.size(); i++){
            if (!files.get(i).startsWith(serverSaveDir + "acc-") || !files.get(i).endsWith(".json")) {
                files.remove(i);
                i--;
            }
        }

        synchronized (allDetails){
            allDetails.clear();
            for (String s : files) {
                File file = new File(s);
                try {
                    String str = Files.readString(file.toPath());
                    Map<String, String> map = (Map<String, String>) gson.fromJson(str, TypeToken.getParameterized(LinkedHashMap.class, String.class, String.class));
                    allDetails.add(map);
                } catch (IOException e) {
                    LOGGER.error("Cannot read or parse file {}", s);
                }
            }
        }

    }

    public static ArrayList<Map<String,String>> getAllDetails() {
        return allDetails;
    }
}

/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jissee.jarsauth.event.EventHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClientDetail {
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, String> map = new HashMap<>();
    public static void add(String key, String value){
        synchronized (map){
            if(key.equals("<end>") && value.equals("<end>")){
                Map<String, String> result = new LinkedHashMap<>();
                map.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered((entry) -> result.put(entry.getKey(), entry.getValue()));
                //FileWriter fw;
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
}

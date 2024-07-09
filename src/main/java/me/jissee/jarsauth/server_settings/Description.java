/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_settings;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Description {
    private static final ArrayList<String> langs = new ArrayList<>();
    private static final String inJar = "assets/jarsauth/desc/";
    private static final String extract = "desc_";

    public static void register(String lang){
        langs.add(lang);
    }
    public static void extract(String serverSaveDir, String lang){
        String inJarPath = inJar + lang + ".txt";
        String extractPath = serverSaveDir + extract + lang + ".txt";

        File file = new File(extractPath);
        boolean overwrite = false;

        try {
            if(!file.exists()){
                file.createNewFile();
                overwrite = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(overwrite){
            try(FileOutputStream fos = new FileOutputStream(file)){
                try (InputStream ins = Description.class.getClassLoader().getResourceAsStream(inJarPath)) {
                    if (ins != null) {
                        fos.write(ins.readAllBytes());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
    public static void extractAll(String serverSaveDir){
        for(String lang : langs){
            extract(serverSaveDir, lang);
        }
    }
}

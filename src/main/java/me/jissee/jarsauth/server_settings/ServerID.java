/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_settings;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.function.Function;

public class ServerID {
    private static final String FILE_NAME = "server-id.txt";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Random random = new Random();
    private final String serverID;
    public ServerID(String serverID){
        this.serverID = serverID;
    }
    public static ServerID load(String serverSaveDir) {
        File file = new File(serverSaveDir + FILE_NAME);

        if(!file.exists()){
            try {
                LOGGER.info("Cannot find " + FILE_NAME + ", generating default setting.");
                file.createNewFile();
                ServerID setting = createDefault();

                FileWriter fw = new FileWriter(file);
                fw.write(setting.getServerID());
                fw.close();

                return setting;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try(FileReader reader = new FileReader(file)){
            int c;
            StringBuilder sb = new StringBuilder();
            while((c = reader.read()) != -1){
                sb.append((char)c);
            }
            String former = sb.substring(0, 80);
            String latter = sb.substring(80);
            Function<String, String> getSMD5 = (str) -> {
                if (str == null || str.length() == 0) {
                    throw new IllegalArgumentException("String to encript cannot be null or zero length");
                }
                StringBuffer hexString = new StringBuffer();
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(str.getBytes());
                    byte[] hash = md.digest();
                    for (int i = 0; i < hash.length; i++) {
                        if ((0xff & hash[i]) < 0x10) {
                            hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                        } else {
                            hexString.append(Integer.toHexString(0xFF & hash[i]));
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                return hexString.toString();
            };

            if (!getSMD5.apply(former).equals(latter)) {
                throw new IllegalArgumentException("server-id.txt is not available. Please remove it manually.");
            }
            return new ServerID(sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ServerID createDefault() {
        StringBuilder sb = new StringBuilder();

        long time = System.nanoTime();
        StringBuilder timeString = new StringBuilder(Long.toHexString(time));
        while(timeString.length() < 16){
            timeString.insert(0, "0");
        }
        sb.append(timeString);
        for(int i = 0; i < 64; i++){
            int integer = random.nextInt(0,16);
            String str = Integer.toHexString(integer);
            sb.append(str);
        }
        String total = sb.toString();

        Function<String, String> getSMD5 = (str) -> {
            if (str == null || str.length() == 0) {
                throw new IllegalArgumentException("String to encript cannot be null or zero length");
            }
            StringBuffer hexString = new StringBuffer();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(str.getBytes());
                byte[] hash = md.digest();
                for (int i = 0; i < hash.length; i++) {
                    if ((0xff & hash[i]) < 0x10) {
                        hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
                    } else {
                        hexString.append(Integer.toHexString(0xFF & hash[i]));
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return hexString.toString();
        };

        String hash = getSMD5.apply(total);

        sb.append(hash);

        return new ServerID(sb.toString());
    }

    public String getServerID(){
        return serverID;
    }
    public String getShortServerID(){
        return serverID.substring(80);
    }
}

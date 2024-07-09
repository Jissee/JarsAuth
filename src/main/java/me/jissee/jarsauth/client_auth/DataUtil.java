/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.client_auth;

import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.event.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.*;
import java.util.Optional;
import java.util.UUID;

public class DataUtil {
    private static final String UUID_FOLDER = "uuid-files";

    @OnlyIn(Dist.CLIENT)
    public static void saveUUID(String serverID, String playerName, UUID uuid) {
        String clientDir = Compatibility.getClientRootDir();
        String fileName = serverID.substring(80) + "." + playerName;
        File folder = new File(clientDir + UUID_FOLDER);
        createFolder(folder);

        File file = new File(clientDir + UUID_FOLDER + File.separator + fileName);
        if(file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
            try(FileOutputStream fos = new FileOutputStream(file)){
                try(ObjectOutputStream oos = new ObjectOutputStream(fos)){
                    oos.writeObject(uuid);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static Optional<UUID> readUUID(String serverID, String playerName) {
        String clientDir = Compatibility.getClientRootDir();
        String fileName = serverID.substring(80) + "." + playerName;
        File folder = new File(clientDir + UUID_FOLDER);
        createFolder(folder);

        File file = new File(clientDir + UUID_FOLDER + File.separator + fileName);
        if(file.exists()){
            try(FileInputStream fis = new FileInputStream(file)){
                try(ObjectInputStream ois = new ObjectInputStream(fis)){
                    Object obj = ois.readObject();
                    return Optional.of((UUID)obj);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            return Optional.empty();
        }
    }
    private static void createFolder(File folder){
        if(!folder.exists()){
            folder.mkdir();
        }
    }
}

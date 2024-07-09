/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license;

import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.server_license.gui.DateParser;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class ServerLicense implements Comparable<ServerLicense>, Serializable {
    @Serial
    private static final long serialVersionUID = -3519660924627942865L;

    public static final int BY_MIN = 114;
    public static final int BY_CONN = 514;
    public static final int INFINITE = 114514;
    public static final String LICENSE_FOLDER = "licenses" + File.separator;

    private String playerName;
    private long startTime;
    private long expireTime;
    private int type;
    private int allowance;
    private volatile boolean canBeRemoved;
    private transient UUID uuid;


    private ServerLicense(){
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getAllowance() {
        return allowance;
    }

    public void setAllowance(int allowance) {
        this.allowance = allowance;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isInEffect(){
        long now = System.currentTimeMillis();
        return now >= startTime && now <= expireTime;
    }
    public boolean hasExpired(){
        long now = System.currentTimeMillis();
        return now > expireTime;
    }
    public boolean canBeRemoved(){
        return canBeRemoved;
    }


    public void write(String serverSaveDir){
        File folder = new File(LICENSE_FOLDER);
        if(!folder.exists()){
            folder.mkdir();
        }
        File file = new File(serverSaveDir + LICENSE_FOLDER + getPlayerName() + "." + uuid.toString() + ".lic");

        try{
            Files.deleteIfExists(file.toPath());
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try(FileOutputStream fos = new FileOutputStream(file)){
            try(ObjectOutputStream oos = new ObjectOutputStream(fos)){
                oos.writeObject(this);
                oos.flush();
            }
            fos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int checkAuth(int lastType, boolean decrease){
        if(hasExpired()){
            canBeRemoved = true;
            return -1;
        }
        if(type == INFINITE && lastType == INFINITE){
            return INFINITE;
        }
        if(lastType == BY_CONN){
            return BY_CONN;
        }
        if(type == BY_CONN || type == BY_MIN){
            if(allowance == 0){
                canBeRemoved = true;
                return -1;
            }
            if(decrease){
                allowance--;
                write(EventHandler.getServerSaveDir());
            }
            return type;
        }
        canBeRemoved = true;
        return -1;
    }

    public static Optional<ServerLicense> readForPlayer(String playerName, String serverSaveDir) {
        ArrayList<ServerLicense> list = new ArrayList<>();

        File folder = new File(serverSaveDir + LICENSE_FOLDER);
        if(!folder.exists()){
            return Optional.empty();
        }
        File[] files = folder.listFiles(((dir, name) -> name.startsWith(playerName + ".")));
        if(files == null){
            return Optional.empty();
        }
        for(File file : files){
            try(FileInputStream fis = new FileInputStream(file)){
                try(ObjectInputStream ois = new ObjectInputStream(fis)){
                    Object obj = ois.readObject();
                    if(obj instanceof ServerLicense license){
                        if(license.isInEffect()){
                            String fileName = file.getName();
                            String[] nameParts = fileName.split("\\.");
                            String uuid = nameParts[1];
                            license.uuid = UUID.fromString(uuid);
                            license.canBeRemoved = false;
                            list.add(license);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        list.sort(ServerLicense::compareTo);
        if(list.isEmpty()){
            return Optional.empty();
        }else{
            return Optional.of(list.get(0));
        }
    }

    public static ServerLicense readFile(String fileName){
        File file = new File(LICENSE_FOLDER + fileName);
        if(!file.exists()){
            try {
                throw new FileNotFoundException();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        try(FileInputStream fis = new FileInputStream(file)){
            try(ObjectInputStream ois = new ObjectInputStream(fis)){
                Object obj = ois.readObject();
                ServerLicense sl = (ServerLicense) obj;
                String[] nameParts = fileName.split("\\.");
                String uuid = nameParts[1];
                sl.uuid = UUID.fromString(uuid);
                return sl;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeFile(String fileName, String serverSaveDir){
        File file = new File(serverSaveDir + LICENSE_FOLDER + fileName);
        if(file.exists()){
            file.delete();
        }
    }

    public static void removeAllInvalid(String serverSaveDir){
        File folder = new File(serverSaveDir + LICENSE_FOLDER);
        File[] files = folder.listFiles();
        if(files == null){
            return;
        }
        ArrayList<String> forRemoval = new ArrayList<>();
        for(File file : files){
            try(FileInputStream fis = new FileInputStream(file)){
                try(ObjectInputStream ois = new ObjectInputStream(fis)){
                    Object obj = ois.readObject();
                    ServerLicense sl = (ServerLicense) obj;
                    if(sl.canBeRemoved()){
                        forRemoval.add(file.getName());
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(!forRemoval.isEmpty()){
            for(String fname : forRemoval){
                removeFile(fname, serverSaveDir);
            }
        }
    }



    @Override
    public String toString() {
        return "ServerLicense{" +
                "playerName='" + playerName + '\'' +
                ", startTime=" + DateParser.format(startTime) +
                ", expireTime=" + DateParser.format(expireTime) +
                ", type=" + (type == BY_MIN ? "BY_MIN" : (type == BY_CONN ? "BY_CONN" : "INFINITE")) +
                ", allowance=" + allowance +
                '}';
    }

    @Override
    public int compareTo(ServerLicense o) {
        return Long.compare(this.expireTime, o.expireTime);
    }



    public static class Builder {
        private final ServerLicense license;
        public Builder(){
            license = new ServerLicense();
        }
        public Builder playerName(String playerName){
            license.setPlayerName(playerName);
            return this;
        }
        public Builder startTime(long startTime){
            license.setStartTime(startTime);
            return this;
        }
        public Builder expireTime(long expireTime){
            license.setExpireTime(expireTime);
            return this;
        }
        public Builder type(int type){
            switch (type){
                case BY_MIN:
                case BY_CONN:
                case INFINITE:
                    license.setType(type);
                    break;
                default:
                    throw new RuntimeException("Wrong type");
            }
            return this;
        }
        public Builder allowance(int allowance){
            license.setAllowance(allowance);
            return this;
        }
        public ServerLicense end(){
            return license;
        }

        public void uuid(UUID uuid) {
            license.uuid = uuid;
        }
    }


}

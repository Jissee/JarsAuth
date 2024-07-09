/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.client_auth;

import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.packet.CABroadcastPacket;
import me.jissee.jarsauth.server_license.SLPendingList;
import me.jissee.jarsauth.server_settings.Settings;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.io.*;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.*;
import java.util.function.Function;

public class CAPendingList {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final CAPendingList instance = new CAPendingList();
    private static Thread independentThread;
    public static CAPendingList getInstance(){
        return instance;
    }

    private final HashMap<String, Record> waiting = new HashMap<>();
    private final ArrayList<Record> loggedIn = new ArrayList<>();
    private final ArrayList<ServerPlayer> newPlayers = new ArrayList<>();

    public void playerLogin(ServerPlayer player){
        String serverID = Settings.getServerID().getServerID();
        String name = player.getName().getString();
        Optional<Record> record = Record.searchFor(name);

        if(record.isEmpty()){
            synchronized (newPlayers){
                newPlayers.add(player);
            }
            if(!Settings.getFileChecksumSetting().isEnabled()){
                allowNew(player);
            }

        }else{
            KeyPair pair;
            try {
                pair = Codec.generateKeyPair();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Record playerRecord = record.get();
            playerRecord.setPrivateKey(pair.getPrivate());
            synchronized (waiting){
                waiting.put(name, playerRecord);
            }

            CABroadcastPacket packet = new CABroadcastPacket(serverID, CABroadcastPacket.PUBLIC_KEY, pair.getPublic().getEncoded());
            Compatibility.sendModPacket(player, packet);

        }
    }

    public void allowNew(ServerPlayer player){
        boolean isNew;
        synchronized (newPlayers){
            isNew = newPlayers.contains(player);
            if(isNew){
                newPlayers.remove(player);
            }
        }

        if(isNew && !Compatibility.hasDisconnected(player)){
            Record record = Record.createAndSaveFor(player.getName().getString());
            String serverID = Settings.getServerID().getServerID();
            String name = player.getName().getString();
            CABroadcastPacket packet = new CABroadcastPacket(serverID, CABroadcastPacket.AUTH_INFO, new byte[]{});
            synchronized (waiting){
                waiting.put(name, record);
            }
            Compatibility.sendModPacket(player, packet);
        }
    }

    private void tick() {
        long millTime = System.currentTimeMillis();
        //remove disconnected player
        synchronized (waiting){
            Set<String> set = waiting.keySet();
            Object[] keys =  set.toArray();
            for(Object obj : keys){
                String name = (String) obj;
                ServerPlayer player = Compatibility.getPlayerByName(name);
                if(Compatibility.hasDisconnected(player)){
                    waiting.remove(obj);
                }
            }
        }
        synchronized (loggedIn){
            int count = loggedIn.size();
            for(int i = 0; i < count; i++){
                String name = loggedIn.get(i).getPlayerName();
                ServerPlayer player = Compatibility.getPlayerByName(name);
                if(Compatibility.hasDisconnected(player)){
                    loggedIn.remove(i);
                    i--;
                }
            }
        }
        synchronized (newPlayers){
            int count = newPlayers.size();
            for(int i = 0; i < count; i++){
                ServerPlayer player = newPlayers.get(i);
                if(Compatibility.hasDisconnected(player)){
                    newPlayers.remove(i);
                    i--;
                }
            }
        }
        //check timeout
        synchronized (waiting){
            Set<String> set = waiting.keySet();
            Object[] keys =  set.toArray();
            for(Object obj : keys){
                String name = (String) obj;
                Record rec = waiting.get(name);
                long timeout = Settings.getClientAuthSetting().getTimeout();

                if(millTime - rec.getLoginTime() > timeout * 1000 && rec.getLoginTime() != 0){
                    ServerPlayer player = Compatibility.getPlayerByName(name);
                    EventHandler.addPlayerToBeRemove(player, Compatibility.translatable("text.caauth.timeout"), 0);
                    waiting.remove(name);
                }
            }
        }
        //handle next authentication
        synchronized (waiting){
            synchronized (loggedIn){
                int count = loggedIn.size();
                for(int i = 0; i < count; i++){
                    millTime = System.currentTimeMillis();
                    Record rec = loggedIn.get(i);
                    String playerName = rec.getPlayerName();
                    long loginTime = rec.getLoginTime();
                    long interval = Settings.getClientAuthSetting().getInterval();
                    if(millTime - loginTime > interval * 1000 && !waiting.containsKey(playerName)){
                        loggedIn.remove(i);
                        i--;

                        Optional<Record> search = Record.searchFor(playerName);
                        if(search.isEmpty()){
                            continue;
                        }

                        rec = search.get();

                        KeyPair pair;
                        try {
                            pair = Codec.generateKeyPair();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        rec.setPrivateKey(pair.getPrivate());

                        waiting.put(rec.playerName, rec);

                        String serverID = Settings.getServerID().getServerID();
                        CABroadcastPacket packet = new CABroadcastPacket(serverID, CABroadcastPacket.PUBLIC_KEY, pair.getPublic().getEncoded());
                        ServerPlayer svplr = Compatibility.getPlayerByName(rec.playerName);

                        Compatibility.sendModPacket(svplr, packet);
                    }
                }
            }
        }
    }

    public static Thread getIndependentThread(){
        if (independentThread == null || !independentThread.isAlive()) {
            independentThread = new Thread(()->{while(true)try{instance.tick();}catch(Exception e){}});
            independentThread.setDaemon(true);
        }
        return independentThread;
    }

    public Optional<UUID> getWaitingPlayerUUID(String name) {
        Record waitingPlayer = waiting.get(name);
        if(waitingPlayer != null){
            return Optional.of(waitingPlayer.getUuid());
        }else{
            return Optional.empty();
        }
    }

    public void addAuthInfo(String serverID, ServerPlayer player, byte[] payload) {
        synchronized (waiting){
            try {
                String playerName = player.getName().getString();
                Record rec = waiting.get(playerName);
                PrivateKey pri = rec.getPrivateKey();
                byte[] uuidDec = Codec.decrypt(payload, pri);
                rec.setPrivateKey(null);
                Function<byte[], byte[]> getBMD5 = data -> {
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        md.update(data);
                        return md.digest();
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("MD5 algorithm not available", e);
                    }
                };
                byte[] uuidBytesHashed = getBMD5.apply(uuidDec);
                UUID uuidHashed = Codec.bytesToUUID(uuidBytesHashed);

                if (uuidHashed.equals(rec.getUuid()) && serverID.equals(Settings.getServerID().getServerID())) {
                    rec.setLoginTime(System.currentTimeMillis());
                    synchronized (loggedIn){
                        loggedIn.add(rec);
                    }
                } else {
                    EventHandler.addPlayerToBeRemove(player, Compatibility.translatable("text.caauth.fail"), 0);
                }
                waiting.remove(playerName);
                LOGGER.debug("Player {} passed CA authentication", playerName);
                SLPendingList.getInstance().allowNew(player);
            } catch (Exception e) {
            }
        }
    }


    private static class Record implements Serializable {
        private static final String UUID_FOLDER = "uuid-files";
        private String playerName;
        private UUID uuid;
        private transient PrivateKey privateKey;
        private transient boolean isNew;
        private transient long loginTime = 0;

        public Record(){}

        public static Optional<Record> searchFor(String playerName){
            Optional<Record> search = searchDiskFor(playerName);
            if(search.isPresent()){
                Record record = search.get();
                record.setNew(false);
                record.setLoginTime(System.currentTimeMillis());
                return Optional.of(record);
            }else{
                return Optional.empty();
            }

        }

        public static Record createAndSaveFor(String playerName){
            Record record1 = new Record();
            record1.setPlayerName(playerName);
            UUID uuidOrigin = UUID.randomUUID();
            byte[] uuidBytesOrigin = Codec.uuidToBytes(uuidOrigin);
            Function<byte[], byte[]> getBMD5 = data -> {
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(data);
                    return md.digest();
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("MD5 algorithm not available", e);
                }
            };
            byte[] uuidBytesHashed = getBMD5.apply(uuidBytesOrigin);
            UUID uuidHashed = Codec.bytesToUUID(uuidBytesHashed);

            record1.setUuid(uuidHashed);
            record1.setNew(true);
            record1.writeToDisk();

            record1.setUuid(uuidOrigin);
            return record1;
        }

        public static Optional<CAPendingList.Record> searchDiskFor(String playerName){
            String serverDir = EventHandler.getServerSaveDir();
            File folder = new File(serverDir + UUID_FOLDER);
            createFolder(folder);

            File file = new File(serverDir + UUID_FOLDER + File.separator + playerName + ".javauuid");
            if(file.exists()){
                try(FileInputStream fis = new FileInputStream(file)){

                    try(ObjectInputStream ois = new ObjectInputStream(fis)){
                        Object obj = ois.readObject();
                        return Optional.of((CAPendingList.Record) obj);
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

        public void writeToDisk() {
            String serverDir = EventHandler.getServerSaveDir();
            //String playerName = record.getPlayerName();
            File folder = new File(serverDir + UUID_FOLDER);
            createFolder(folder);

            File file = new File(serverDir + UUID_FOLDER + File.separator + getPlayerName() + ".javauuid");
            try {
                file.createNewFile();
                try(FileOutputStream fos = new FileOutputStream(file)){
                    try(ObjectOutputStream oos = new ObjectOutputStream(fos)){
                        oos.writeObject(this);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        private static void createFolder(File folder){
            if(!folder.exists()){
                folder.mkdir();
            }
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
        }

        public boolean isNew() {
            return isNew;
        }

        public void setNew(boolean aNew) {
            isNew = aNew;
        }

        public long getLoginTime() {
            return loginTime;
        }

        public void setLoginTime(long loginTime) {
            this.loginTime = loginTime;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

    }
}

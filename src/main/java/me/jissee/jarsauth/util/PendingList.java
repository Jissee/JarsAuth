/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.packet.BroadcastPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import me.jissee.jarsauth.profile.ClientProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class PendingList {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String NONE = "none";
    private static final String CALCULATING = "calculating";
    private static final String FOLDER = "folder";
    private static final PendingList instance = new PendingList();
    private static Thread independentThread;
    public static PendingList getInstance(){
        return instance;
    }
    private final Object LOCK = new Object();
    private final ArrayList<Record> records = new ArrayList<>();

    public void add(ServerPlayer player) {
        synchronized (LOCK){
            if(EventHandler.getServerSaveDir() != null){
                Record r = new Record(player, 0, NONE, NONE, NONE, NONE);
                for(Record r1 : records){
                    if(r.equals(r1)){
                        break;
                    }
                }
                records.add(r);
            }
        }
    }



    public void addHash1(ServerPlayer player, String hash){
        synchronized (LOCK){
            for(int i = 0; i < records.size(); i++){
                Record r = records.get(i);
                synchronized (r){
                    if(r.player.getId() == player.getId()){
                        r.got1 = hash;
                        break;
                    }
                }
            }
        }
    }

    public void addHash2(ServerPlayer player, String hash){
        synchronized (LOCK){
            for(int i = 0; i < records.size(); i++){
                Record r = records.get(i);
                synchronized (r){
                    if(r.player.getId() == player.getId()){
                        r.got2 = hash;
                        break;
                    }
                }
            }
        }
    }


    public void tick() {
        synchronized (LOCK) {
            for (int i = 0; i < records.size(); i++) {
                Record r = records.get(i);
                synchronized (r) {
                    if (r.player == null || r.player.hasDisconnected()) {
                        records.remove(r);
                        i--;
                    }
                }
            }
        }

        synchronized (LOCK) {
            for (int i = 0; i < records.size(); i++) {
                Record r = records.get(i);
                synchronized (r){
                    long timeout = EventHandler.getProfile().getTimeout();
                    //时间超过超时时间
                    if (System.nanoTime() - r.time > timeout * 1000 * 1000 * 1000
                            && (r.got1.equals(NONE) || r.got2.equals(NONE)) //仍未收到客户端的反馈
                            && !r.expected1.equals(NONE)                    //服务端完成计算
                            && !r.expected1.equals(CALCULATING)
                            && !r.expected2.equals(NONE)
                            && !r.expected2.equals(CALCULATING)
                    ) {
                        r.player.connection.disconnect(Component.translatable("text.auth.timeout"));
                        records.remove(r);
                        i--;
                        continue;
                    }

                    if (!r.expected1.equals(NONE)                //服务端完成计算
                            && !r.expected2.equals(NONE)
                            && !r.expected1.equals(CALCULATING)
                            && !r.expected2.equals(CALCULATING)
                            && !r.got1.equals(NONE)             //客户端完成计算
                            && !r.got2.equals(NONE)
                    ) {
                        BiPredicate<String, String> checkAuth = (expected, got) -> {
                            try {
                                for (int x = 0; x < expected.length(); x += 32) {
                                    if (expected.substring(x, x + 32).equals(got)) {
                                        return true;
                                    }
                                }
                            } catch (Exception e) {
                                return false;
                            }
                            return false;
                        };
                        ServerPlayer player = r.player;
                        if (checkAuth.test(r.expected1, r.got1) && checkAuth.test(r.expected2, r.got2)) {
                            r.time = System.nanoTime();
                            r.expected1 = NONE;
                            r.got1 = NONE;
                            r.expected2 = NONE;
                            r.got2 = NONE;
                            LOGGER.debug(r.player.getName().getString() + " passed authentication");
                        } else {
                            ClientProfile cp = EventHandler.getProfile();
                            ArrayList<String> msgs = cp.getRefuseMessage();
                            StringBuilder sb = new StringBuilder();
                            for (String str : msgs) {
                                sb.append(str);
                                sb.append("\n");
                            }
                            player.connection.disconnect(Component.literal(sb.toString()));
                            records.remove(r);
                            i--;
                        }
                    }
                }
            }
        }

        synchronized (LOCK) {
            for (Record r : records) {
                synchronized (r) {
                    long interval =  EventHandler.getProfile().getInterval();
                    if (r.expected1.equals(NONE)    //服务端未完成计算
                            //时间超过间隔时间
                            && System.nanoTime() - r.time > interval * 1000 * 1000 * 1000
                    ) {
                        Supplier<String> getRandom = () -> {
                            StringBuilder sb = new StringBuilder();
                            for(int i = 0; i < 32; i++){
                                sb.append((char)(Math.random() * 95 + 32));
                            }
                            return sb.toString();
                        };
                        String salt1 = getRandom.get();
                        String salt2 = getRandom.get();

                        Thread thread = new Thread(() -> {
                            Function<String, String> getMD5 = (str) -> {
                                /* Credit for "https://blog.csdn.net/u012660464/article/details/78759296" */
                                if (str == null || str.length() == 0) {
                                    throw new IllegalArgumentException("String to encript cannot be null or zero length");
                                }
                                StringBuilder hexString = new StringBuilder();
                                try {
                                    MessageDigest md = MessageDigest.getInstance("MD5");
                                    md.update(str.getBytes());
                                    byte[] hash = md.digest();
                                    for (int j = 0; j < hash.length; j++) {
                                        if ((0xff & hash[j]) < 0x10) {
                                            hexString.append("0" + Integer.toHexString((0xFF & hash[j])));
                                        } else {
                                            hexString.append(Integer.toHexString(0xFF & hash[j]));
                                        }
                                    }
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                return hexString.toString();
                            };
                            ClientProfile prof = EventHandler.getProfile();
                            ArrayList<Map<String, String>> allDetails = EventHandler.getAllDetails();

                            StringBuilder total1 = new StringBuilder();
                            StringBuilder total2 = new StringBuilder();

                            ArrayList<String> inclStr = new ArrayList<>();
                            ArrayList<String> types = new ArrayList<>();

                            StringBuilder folders = new StringBuilder();
                            StringBuilder files = new StringBuilder();

                            for(Map<String, String> theMap: allDetails){
                                ArrayList<String> rawIncl = prof.getInclusion();

                                inclStr.clear();
                                types.clear();

                                folders.setLength(0);
                                files.setLength(0);

                                for(String incl : rawIncl){
                                    int lastpos = incl.lastIndexOf('/');
                                    String former;
                                    String latter;
                                    if(lastpos > -1){
                                        former = incl.substring(0, lastpos);
                                        latter = incl.substring(lastpos + 1);
                                    }else{
                                        former = "";
                                        latter = incl;
                                    }

                                    try{
                                        if(latter.equals("*")){
                                            String processedValue = theMap.get(former);
                                            if(processedValue != null && processedValue.equals(FOLDER)){
                                                inclStr.add(former + '/');
                                                types.add("*");
                                            }
                                        }else{
                                            String processedValue = theMap.get(incl);
                                            if (processedValue != null) {
                                                if(processedValue.equals(FOLDER)){
                                                    inclStr.add(incl + '/');
                                                    types.add(FOLDER);
                                                }else{
                                                    inclStr.add(incl);
                                                    types.add("");
                                                }
                                            }
                                        }
                                    }catch(NullPointerException e){
                                        LOGGER.error("Error while loading client detail.", e);
                                    }
                                }

                                for(Map.Entry<String, String> entry : theMap.entrySet()){
                                    String k = entry.getKey();
                                    String v = entry.getValue();
                                    if(v.equals(FOLDER)) k = k + '/';
                                    for (int i = 0; i < inclStr.size(); i++) {
                                        String incl = inclStr.get(i);
                                        String type = types.get(i);
                                        if(k.startsWith(incl)){
                                            if(type.equals("*")){
                                                if(v.equals(FOLDER)){
                                                    folders.append(k).append(v).append('\n');
                                                }else{
                                                    files.append(k).append(v).append('\n');
                                                }
                                            }else if(k.lastIndexOf("/") == incl.lastIndexOf("/")){
                                                if(v.equals(FOLDER)){
                                                    folders.append(k).append(v).append('\n');
                                                }else{
                                                    files.append(k).append(v).append('\n');
                                                }
                                            }
                                        }
                                    }
                                }
                                String finalStr = folders.append(files).toString();
                                LOGGER.debug(finalStr);
                                total1.append(getMD5.apply(finalStr + salt1));
                                total2.append(getMD5.apply(finalStr + salt2));
                            }
                            synchronized (r) {
                                r.expected1 = total1.toString();
                                r.expected2 = total2.toString();
                            }

                        });
                        r.expected1 = CALCULATING;
                        r.expected2 = CALCULATING;
                        thread.start();

                        ClientProfile prof = EventHandler.getProfile();
                        ArrayList<String> incl = prof.getInclusion();
                        String str = gson.toJson(incl);
                        r.player.connection.send(new ClientboundResourcePackPacket(str, "JARSAUTH AUTHENTICATION INFORMATI0N", false, Component.literal(salt1)));
                        PacketHandler.sendToPlayer(new BroadcastPacket(str, "JARSAUTH AUTHENTICATION INFORMATI0N", Component.literal(salt2)), r.player);
                        r.time = System.nanoTime();
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

    private static class Record {
        private final ServerPlayer player;
        private volatile long time;
        private volatile String expected1;
        private volatile String got1;
        private volatile String expected2;
        private volatile String got2;
        private Record(ServerPlayer player, long time, String expected1, String got1, String expected2, String got2){
            this.player = player;
            this.time = time;
            this.expected1 = expected1;
            this.got1 = got1;
            this.expected2 = expected2;
            this.got2 = got2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(player, record.player);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player);
        }
    }

}


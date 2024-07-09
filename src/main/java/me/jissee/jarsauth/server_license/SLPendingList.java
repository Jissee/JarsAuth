/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license;

import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.server_settings.Settings;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.*;

public class SLPendingList {
    private static final int MILL_PER_MIN = 60 * 1000;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SLPendingList instance = new SLPendingList();
    private static Thread independentThread;
    public static SLPendingList getInstance(){
        return instance;
    }

    private final ArrayList<Record> records = new ArrayList<>();
    private final ArrayList<ServerPlayer> newPlayers = new ArrayList<>();

    private long lastRemoveAllTime;

    private SLPendingList(){
        if(Settings.getServerLicenseSetting().isAutoRemove()){
            lastRemoveAllTime = System.currentTimeMillis();
            ServerLicense.removeAllInvalid(EventHandler.getServerSaveDir());
        }
    }

    public void playerLogin(ServerPlayer serverPlayer){
        synchronized (newPlayers){
            if(!newPlayers.contains(serverPlayer)){
                newPlayers.add(serverPlayer);
            }
        }
        if(!Settings.getClientAuthSetting().isEnabled() && !Settings.getFileChecksumSetting().isEnabled()){
            allowNew(serverPlayer);
        }
    }
    public void allowNew(ServerPlayer serverPlayer){
        boolean isNew;
        synchronized (newPlayers){
            isNew = newPlayers.contains(serverPlayer);
            if(isNew){
                newPlayers.remove(serverPlayer);
            }
        }
        if(isNew && !Compatibility.hasDisconnected(serverPlayer)){
            Record record = new Record(serverPlayer);
            synchronized (records){
                if(!records.contains(record)){
                    records.add(record);
                }
            }
        }
    }
    
    public static Thread getIndependentThread(){
        if (independentThread == null || !independentThread.isAlive()) {
            independentThread = new Thread(()->{while(true)try{instance.tick();}catch(Exception e){ LOGGER.error("SLPending", e);}});
            independentThread.setDaemon(true);
        }
        return independentThread;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        int apm = Settings.getServerLicenseSetting().getAuthPerMinute();
        long interval = MILL_PER_MIN / apm;
        if(Settings.getServerLicenseSetting().isAutoRemove() && now - lastRemoveAllTime > interval){
            lastRemoveAllTime = System.currentTimeMillis();
            ServerLicense.removeAllInvalid(EventHandler.getServerSaveDir());
        }
        synchronized (newPlayers){
            for(int i = 0; i < newPlayers.size(); i++){
                ServerPlayer player = newPlayers.get(i);
                if(Compatibility.hasDisconnected(player)){
                    newPlayers.remove(i);
                    i--;
                }
            }
        }
        synchronized (records){
            for(int i = 0; i < records.size(); i++){
                Record rec = records.get(i);
                ServerPlayer player = rec.getPlayer();
                if(Compatibility.hasDisconnected(player)){
                    records.remove(i);
                    i--;
                }
            }
        }
        synchronized (records){
            for(int i = 0; i < records.size(); i++){
                Record rec = records.get(i);
                if(rec.hasGeneralAllowance()){
                }else{
                    boolean result = rec.checkAuth();
                    if(!result){
                        EventHandler.addPlayerToBeRemove(rec.getPlayer(), Compatibility.translatable("text.slauth.fail"), 1);
                        records.remove(i);
                        i--;
                    }
                }
            }
        }
    }

    private static class Record {

        private final ServerPlayer player;
        private final long loginTime;
        private long lastAuthTime = 0;
        private long lastDecreaseTime = 0;
        private int lastType = 0;
        public Record(ServerPlayer player){
            this.player = player;
            loginTime = System.currentTimeMillis();
        }
        public boolean hasGeneralAllowance(){
            long now = System.currentTimeMillis();
            long diff = now - loginTime;
            boolean result = diff < Settings.getServerLicenseSetting().getGeneralAllowance() * 1000L;
            if(!result && lastAuthTime == 0){
                int apm = Settings.getServerLicenseSetting().getAuthPerMinute();
                long interval = MILL_PER_MIN / apm;
                lastAuthTime = System.currentTimeMillis() - interval;
                lastDecreaseTime = System.currentTimeMillis() - MILL_PER_MIN;
            }
            return result;
        }
        public boolean checkAuth(){
            long now = System.currentTimeMillis();
            long diff = now - lastAuthTime;
            int apm = Settings.getServerLicenseSetting().getAuthPerMinute();
            long interval = MILL_PER_MIN / apm;

            if(diff > interval && lastAuthTime > 0){
                String playerName = player.getName().getString();
                Optional<ServerLicense> foundLicense = ServerLicense.readForPlayer(playerName, EventHandler.getServerSaveDir());
                lastAuthTime = now;
                if(foundLicense.isEmpty()){
                    return false;
                }else{
                    boolean decrease = false;
                    if(now - lastDecreaseTime > MILL_PER_MIN){
                        decrease = true;
                        lastDecreaseTime = now;
                    }
                    lastType = foundLicense.get().checkAuth(lastType, decrease);
                    boolean result = lastType != -1;
                    if(result){
                        LOGGER.debug("Player {} passed SL authentication", playerName);
                    }
                    return result;
                }
            }else {
                return true;
            }
        }
        public ServerPlayer getPlayer(){
            return player;
        }
        public long getLastAuthTime(){
            return lastAuthTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Record record)) return false;
            return Objects.equals(player.getName().getString(), record.player.getName().getString());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(player.getName().getString());
        }
    }
}

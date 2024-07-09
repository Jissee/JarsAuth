/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.client_auth.CAPendingList;
import me.jissee.jarsauth.file_checksum.FCPendingList;
import me.jissee.jarsauth.packet.FCBroadcastPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import me.jissee.jarsauth.server_license.SLPendingList;
import me.jissee.jarsauth.server_license.gui.JarCopyTool;
import me.jissee.jarsauth.server_settings.ClientDetail;
import me.jissee.jarsauth.server_settings.Description;
import me.jissee.jarsauth.server_settings.Settings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static String serverSaveDir;

    private static final AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode = new AtomicBoolean();
    private static final Queue<ServerPlayer> kickList = new ArrayDeque<>();
    private static final Queue<Component> reasons = new ArrayDeque<>();
    private static final Queue<IntHolder> delayTicks = new ArrayDeque<>();
    private static DedicatedServer server;
    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event){
        MinecraftServer server = event.getServer();
        if(server instanceof DedicatedServer dserver){
            serverSaveDir = server.getWorldPath(LevelResource.ROOT) + File.separator;
            reloadSettings();
            reloadDetails();
            FCPendingList.getIndependentThread().start();
            CAPendingList.getIndependentThread().start();
            SLPendingList.getIndependentThread().start();
            Description.extractAll(serverSaveDir);
            EventHandler.server = dserver;

            File jar = JarCopyTool.getJarFile();

            Path dest = Path.of(serverSaveDir + jar.getName());

            if(jar.isFile() && jar.exists() && !Files.exists(dest)){
                try {
                    Files.copy(jar.toPath(), dest);
                } catch (IOException e) {
                    LOGGER.error("Cannot copy jar file", e);
                }
            }
        }
    }
    public static void registerPackets(IEventBus bus){
        PacketHandler.registerAll(bus);
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        Player plr = event.getEntity();
        if(plr instanceof ServerPlayer svplr){

            if(Settings.getFileChecksumSetting().isEnabled()){
                if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                    FCBroadcastPacket packet = new FCBroadcastPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", Optional.empty());
                    Compatibility.sendModPacket(svplr, packet);
                }else{
                    FCPendingList.getInstance().playerLogin(svplr);
                }
            }

            if(Settings.getClientAuthSetting().isEnabled()){
                CAPendingList.getInstance().playerLogin(svplr);
            }

            if(Settings.getServerLicenseSetting().isEnabled()){
                SLPendingList.getInstance().playerLogin(svplr);
            }
        }
    }
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event){
        synchronized (kickList){
            while(!kickList.isEmpty()){
                ServerPlayer player = kickList.peek();
                Component reason = reasons.peek();
                IntHolder delayTick = delayTicks.peek();

                assert delayTick != null;
                int value = delayTick.getValue();
                if(value > 0){
                    delayTick.setValue(value - 1);
                }else{
                    if(reason == null){
                        reason = Compatibility.literal("");
                    }
                    Compatibility.disconnectWithReason(player, reason);
                    kickList.poll();
                    reasons.poll();
                    delayTicks.poll();
                }
            }
        }
    }

    public static void addPlayerToBeRemove(ServerPlayer player, Component reason, int delayTick){
        synchronized (kickList){
            kickList.add(player);
            reasons.add(reason);
            delayTicks.add(new IntHolder(delayTick));
        }
    }

    public static String getServerSaveDir() {
        return serverSaveDir;
    }

    public static void reloadSettings(){
        Settings.loadAllSettings(serverSaveDir);
        Settings.printAll();
        LOGGER.info("Settings reloaded");
    }

    public static void reloadDetails(){
        ClientDetail.reloadDetails(serverSaveDir);
        LOGGER.info("Client details reloaded");
    }

    public static AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode() {
        return ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode;
    }

    public static DedicatedServer getServer(){
        return server;
    }
    private static class IntHolder{
        private int value;
        private IntHolder(int value){
            this.value = value;
        }
        private void setValue(int value){
            this.value = value;
        }
        private int getValue(){
            return value;
        }
    }
}

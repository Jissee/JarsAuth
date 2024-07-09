/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.client_auth.CAPendingList;
import me.jissee.jarsauth.command.ModCommand;
import me.jissee.jarsauth.file_checksum.FCPendingList;
import me.jissee.jarsauth.packet.FCBroadcastPacket;
import me.jissee.jarsauth.server_license.SLPendingList;
import me.jissee.jarsauth.server_license.gui.JarCopyTool;
import me.jissee.jarsauth.server_settings.ClientDetail;
import me.jissee.jarsauth.server_settings.Description;
import me.jissee.jarsauth.server_settings.Settings;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


public class EventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static String serverSaveDir;

    private static final AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode = new AtomicBoolean();
    private static final Queue<ServerPlayerEntity> kickList = new ArrayDeque<>();
    private static final Queue<Text> reasons = new ArrayDeque<>();
    private static final Queue<EventHandler.IntHolder> delayTicks = new ArrayDeque<>();
    private static MinecraftDedicatedServer server;
    public static void register(){
        CommandRegistrationCallback.EVENT.register(EventHandler::onCommandRegistration);
        ServerPlayConnectionEvents.JOIN.register(EventHandler::onPlayerLoggedIn);
        ServerLifecycleEvents.SERVER_STARTED.register(EventHandler::onServerStarted);
        ServerTickEvents.START_SERVER_TICK.register(EventHandler::onServerTick);
    }

    private static void onServerStarted(
            MinecraftServer minecraftServer
    ) {
        MinecraftServer server = minecraftServer;
        if(server instanceof MinecraftDedicatedServer dserver){
            serverSaveDir = server.getSavePath(WorldSavePath.ROOT) + File.separator;
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


    private static void onCommandRegistration(
            CommandDispatcher<ServerCommandSource> serverCommandManagerourceCommandDispatcher,
            CommandRegistryAccess commandRegistryAccess,
            CommandManager.RegistrationEnvironment registrationEnvironment
    ) {
        ModCommand.onRegisterCommand(serverCommandManagerourceCommandDispatcher);
    }
    private static void onPlayerLoggedIn(
            ServerPlayNetworkHandler serverPlayNetworkHandler,
            PacketSender packetSender,
            MinecraftServer minecraftServer
    ) {

        ServerPlayerEntity svplr = serverPlayNetworkHandler.player;
        Compatibility.addPlayerSender(svplr.getName().getString(), packetSender);

        //if(plr instanceof ServerPlayerEntity svplr){
            if(Settings.getFileChecksumSetting().isEnabled()){
                if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                    FCBroadcastPacket packet = new FCBroadcastPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", null);
                    Compatibility.sendModPacket(packetSender, packet);
                }else{
                    FCPendingList.getInstance().playerLogin(svplr, packetSender);
                }
            }

            if(Settings.getClientAuthSetting().isEnabled()){
                CAPendingList.getInstance().playerLogin(svplr, packetSender);
            }

            if(Settings.getServerLicenseSetting().isEnabled()){
                SLPendingList.getInstance().playerLogin(svplr, packetSender);
            }
        //}
    }

    public static void onServerTick(MinecraftServer server){
        synchronized (kickList){
            while(!kickList.isEmpty()){
                ServerPlayerEntity player = kickList.peek();
                Text reason = reasons.peek();
                EventHandler.IntHolder delayTick = delayTicks.peek();

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

    public static void addPlayerToBeRemove(ServerPlayerEntity player, Text reason, int delayTick){
        synchronized (kickList){
            kickList.add(player);
            reasons.add(reason);
            delayTicks.add(new EventHandler.IntHolder(delayTick));
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

    public static MinecraftDedicatedServer getServer(){
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

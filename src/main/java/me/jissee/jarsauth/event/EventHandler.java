/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.packet.BroadcastPacket;
import me.jissee.jarsauth.profile.ClientProfile;
import me.jissee.jarsauth.util.PendingList;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.network.PacketByteBuf;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;


public class EventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static String serverSaveDir;
    private static ClientProfile profile;
    private static final ArrayList<Map<String,String>> allDetails = new ArrayList<>();
    private static final AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode = new AtomicBoolean();
    private static final Queue<ServerPlayerEntity> kickList = new ArrayDeque<>();
    private static final Queue<Text> reasons = new ArrayDeque<>();
    public static void register(){
        CommandRegistrationCallback.EVENT.register(EventHandler::onCommandRegistration);
        ServerPlayConnectionEvents.JOIN.register(EventHandler::onPlayerLoggenIn);
        ServerLifecycleEvents.SERVER_STARTED.register(EventHandler::onServerStarting);
        ServerTickEvents.START_SERVER_TICK.register(EventHandler::onServerTick);
    }

    private static void onServerStarting(
            MinecraftServer minecraftServer
    ) {
        if(minecraftServer instanceof MinecraftDedicatedServer dserver){
            serverSaveDir = minecraftServer.getSavePath(WorldSavePath.ROOT) + File.separator;
            reloadProfile();
            reloadDetails();
            Thread t = PendingList.getIndependentThread();
            if(!t.isAlive()){
                t.start();
            }
        }
    }


    private static void onCommandRegistration(
            CommandDispatcher<ServerCommandSource> serverCommandManagerourceCommandDispatcher,
            CommandRegistryAccess commandRegistryAccess,
            CommandManager.RegistrationEnvironment registrationEnvironment
    ) {
        onRegisterCommand(serverCommandManagerourceCommandDispatcher);
    }
    private static void onPlayerLoggenIn(
            ServerPlayNetworkHandler serverPlayNetworkHandler,
            PacketSender packetSender,
            MinecraftServer minecraftServer
    ) {
        ServerPlayerEntity svplr = serverPlayNetworkHandler.player;
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                //new(auth profile, auth flag, true, null)
                BroadcastPacket packet = new BroadcastPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", null);
                PacketByteBuf buf = PacketByteBufs.create();
                packet.encode(buf);
                packetSender.sendPacket(BroadcastPacket.BROADCAST_PACKET, buf);
                //PacketHandler.sendToPlayer(new BroadcastPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", null), svplr);
                //svplr.connection.send(new ClientboundResourcePackPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", true, null));
            }else{
                PendingList.getInstance().add(svplr);
            }
        }
    }

    public static void onRegisterCommand(CommandDispatcher<ServerCommandSource> event){
        LiteralArgumentBuilder<ServerCommandSource> JARSAUTH = CommandManager.literal("jarsauth");

        JARSAUTH.then(
                CommandManager.literal("record").requires((req) -> FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && req.hasPermissionLevel(4))
                        .executes((src) -> {
                            if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                                EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(false);
                                src.getSource().sendMessage(Text.translatable("text.record.off"));
                            }else{
                                EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(true);
                                src.getSource().sendMessage(Text.translatable("text.record.on"));
                            }
                            return 0;
                        })
        );
        JARSAUTH.then(
                CommandManager.literal("reload").requires((req) -> FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && req.hasPermissionLevel(4))
                        .executes((src)->{
                            EventHandler.reloadProfile();
                            EventHandler.reloadDetails();
                            return 0;
                        })
        );
        JARSAUTH.then(
                CommandManager.literal("help").requires((req) -> FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && req.hasPermissionLevel(4))
                        .executes((src)->{
                            src.getSource().sendMessage(Text.literal("/jarsauth record  打开或关闭记录模式"));
                            src.getSource().sendMessage(Text.literal("/jarsauth reload  重新加载配置"));
                            return 0;
                        })
        );
        event.register(JARSAUTH);
    }

    public static void onServerTick(MinecraftServer server){
        synchronized (kickList){
            while(!kickList.isEmpty()){
                ServerPlayerEntity player = kickList.poll();
                Text reason = reasons.poll();
                if(reason == null){
                    reason = Text.literal("");
                }
                player.networkHandler.disconnect(reason);
            }
        }
    }

    public static void addPlayerToBeRemove(ServerPlayerEntity player, Text reason){
        synchronized (kickList){
            kickList.add(player);
            reasons.add(reason);
        }
    }

    public static String getServerSaveDir() {
        return serverSaveDir;
    }
    public static void reloadProfile(){
        profile = ClientProfile.load(serverSaveDir);
        LOGGER.info("Config profile reloaded");
    }
    public static ClientProfile getProfile(){
        return profile;
    }

    public static AtomicBoolean ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode() {
        return ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode;
    }
    public static void reloadDetails(){
        Function<File, ArrayList<String>> getFilesInFolder = (dir) -> {
            ArrayList<String> result = new ArrayList<>();
            if (!dir.exists() || dir.isFile()) {
                return result;
            } else if (dir.isDirectory()) {
                for (File fl : dir.listFiles()) {
                    if (fl.isFile()) {
                        result.add(fl.getPath());
                    }
                }
            }
            return result;
        };
        ArrayList<String> files = getFilesInFolder.apply(new File(serverSaveDir));
        for(int i = 0; i < files.size(); i++){
            if (!files.get(i).startsWith(serverSaveDir + "acc-") || !files.get(i).endsWith(".json")) {
                files.remove(i);
                i--;
            }
        }

        synchronized (allDetails){
            for (String s : files) {
                JsonObject jobj = new JsonObject();
                File file = new File(s);
                try {
                    String str = Files.readString(file.toPath());
                    Map<String, String> map = (Map<String, String>) gson.fromJson(str, TypeToken.getParameterized(LinkedHashMap.class, String.class, String.class));
                    allDetails.add(map);
                } catch (IOException e) {
                    LOGGER.error("Cannot read file.", e);
                }
            }
        }
        LOGGER.info("Client details reloaded");
    }

    public static ArrayList<Map<String,String>> getAllDetails() {
        return allDetails;
    }
}

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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.packet.BroadcastPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import me.jissee.jarsauth.profile.ClientProfile;
import me.jissee.jarsauth.util.PendingList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
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
    private static final Queue<ServerPlayer> kickList = new ArrayDeque<>();
    private static final Queue<Component> reasons = new ArrayDeque<>();
    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event){
        MinecraftServer server = event.getServer();
        if(server instanceof DedicatedServer dserver){
            serverSaveDir = server.getWorldPath(LevelResource.ROOT) + File.separator;
            reloadProfile();
            reloadDetails();
            PendingList.getIndependentThread().start();
        }
    }
    public static void onCommonSetup(FMLCommonSetupEvent event){
        PacketHandler.register();
    }
    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event){
        LiteralArgumentBuilder<CommandSourceStack> JARSAUTH = Commands.literal("jarsauth");

        JARSAUTH.then(
                Commands.literal("record").requires((req) -> FMLLoader.getDist().isDedicatedServer() && req.hasPermission(4))
                        .executes((src) -> {
                            if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                                EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(false);
                                src.getSource().sendSystemMessage(Component.translatable("text.record.off"));
                            }else{
                                EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(true);
                                src.getSource().sendSystemMessage(Component.translatable("text.record.on"));
                            }
                            return 0;
                        })
        );
        JARSAUTH.then(
                Commands.literal("reload").requires((req) -> FMLLoader.getDist().isDedicatedServer() && req.hasPermission(4))
                        .executes((src)->{
                            EventHandler.reloadProfile();
                            EventHandler.reloadDetails();
                            return 0;
                        })
        );
        JARSAUTH.then(
                Commands.literal("help").requires((req) -> FMLLoader.getDist().isDedicatedServer() && req.hasPermission(4))
                        .executes((src)->{
                            src.getSource().sendSystemMessage(Component.literal("/jarsauth record  打开或关闭记录模式"));
                            src.getSource().sendSystemMessage(Component.literal("/jarsauth reload  重新加载配置"));
                            return 0;
                        })
        );
        event.getDispatcher().register(JARSAUTH);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        Player plr = event.getEntity();
        if(plr instanceof ServerPlayer){
            ServerPlayer svplr = (ServerPlayer) plr;
            if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                //new(auth profile, auth flag, true, null)
                PacketHandler.sendToPlayer(new BroadcastPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", null), svplr);
                //svplr.connection.send(new ClientboundResourcePackPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", true, null));
            }else{
                PendingList.getInstance().add(svplr);
            }
        }
    }
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event){
        synchronized (kickList){
            while(!kickList.isEmpty()){
                ServerPlayer player = kickList.poll();
                Component reason = reasons.poll();
                if(reason == null){
                    reason = Component.literal("");
                }
                player.connection.disconnect(reason);
            }
        }
    }

    public static void addPlayerToBeRemove(ServerPlayer player, Component reason){
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

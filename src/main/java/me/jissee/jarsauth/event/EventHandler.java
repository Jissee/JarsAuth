package me.jissee.jarsauth.event;

import me.jissee.jarsauth.JarsAuth;
import me.jissee.jarsauth.ModCommand;
import me.jissee.jarsauth.ThreadExecutor;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.config.VolatileConfig;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.pending.AbstractPendingList;
import me.jissee.jarsauth.pending.CAPendingList;
import me.jissee.jarsauth.pending.FCPendingList;
import me.jissee.jarsauth.pending.SLPendingList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static com.mojang.text2speech.Narrator.LOGGER;

public class EventHandler {
    private static final Queue<ServerPlayer> kickList = new ArrayDeque<>();
    private static final Queue<Component> reasons = new ArrayDeque<>();
    private static final Queue<IntHolder> delayTicks = new ArrayDeque<>();

    private static final Map<Class<? extends AbstractPendingList<?>>, AbstractPendingList<?>> pendingLists = new ConcurrentHashMap<>();
    private static final DataManager dataManager = DataManager.getServerInstance();
    private static final ConfigService configService = dataManager.getService(ConfigService.class);

    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event){
        MinecraftServer server = event.getServer();
        if(server instanceof DedicatedServer){
            ConfigService configService = DataManager.getServerInstance().getService(ConfigService.class);
            long fce = configService.getValue(ConfigKey.FILE_CHECKSUM_ENABLED);
            long cae = configService.getValue(ConfigKey.CLIENT_AUTH_ENABLED);
            long sle = configService.getValue(ConfigKey.SERVER_LICENSE_ENABLED);
            if(fce != 0){
                pendingLists.put(FCPendingList.class, new FCPendingList(server));
            }
            if(cae != 0){
                pendingLists.put(CAPendingList.class, new CAPendingList(server));
            }
            if(sle != 0){
                pendingLists.put(SLPendingList.class, new SLPendingList(server));
            }
            File jar = JarsAuth.getJarFile();

            Path dest = Path.of("./" + jar.getName());

            if(jar.isFile() && jar.exists() && !Files.exists(dest)){
                try {
                    Files.copy(jar.toPath(), dest);
                } catch (IOException e) {
                    LOGGER.error("Cannot copy jar file", e);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event){
        ThreadExecutor.getInstance().shutdown();
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        Player plr = event.getEntity();
        if(plr instanceof ServerPlayer svplr){

            if(configService.getValue(ConfigKey.FILE_CHECKSUM_ENABLED) == 1){
                if(VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                    if(Objects.requireNonNull(svplr.getServer()).getPlayerCount() > 1){
                        svplr.connection.disconnect(Component.translatable("text.disconn.recording"));
                    }else{
                        Scoreboard scoreboard = new Scoreboard();
                        int flag = -114;
                        PlayerTeam team = new PlayerTeam(scoreboard, "!!$%!$" + flag);
                        ClientboundSetPlayerTeamPacket packet =
                                ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
                        svplr.connection.send(packet);
                    }
                }else{
                    getPendingList(FCPendingList.class).addUser(svplr.getUUID());
                }
            }
            if(configService.getValue(ConfigKey.CLIENT_AUTH_ENABLED) == 1){
                getPendingList(CAPendingList.class).addUser(svplr.getUUID());
            }
            if(configService.getValue(ConfigKey.SERVER_LICENSE_ENABLED) == 1){
                getPendingList(SLPendingList.class).addUser(svplr.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event){
        Player plr = event.getEntity();
        if(plr instanceof ServerPlayer svplr){
            pendingLists.values().forEach(pendingList -> pendingList.removeUser(svplr.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event){
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
                        reason = Component.literal("");
                    }
                    player.connection.disconnect(reason);
                    kickList.poll();
                    reasons.poll();
                    delayTicks.poll();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event){
        ModCommand.register(event.getDispatcher());
    }

    public static void addPlayerToBeRemove(ServerPlayer player, Component reason, int delayTick){
        synchronized (kickList){
            if(player == null) return;
            kickList.add(player);
            reasons.add(reason);
            delayTicks.add(new IntHolder(delayTick));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractPendingList<?>> T getPendingList(Class<T> clazz){
        return (T) pendingLists.get(clazz);
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

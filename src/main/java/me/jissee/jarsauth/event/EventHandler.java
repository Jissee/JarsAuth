package me.jissee.jarsauth.event;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.config.VolatileConfig;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.pending.CAPendingList;
import me.jissee.jarsauth.pending.FCPendingList;
import me.jissee.jarsauth.pending.SLPendingList;
import me.jissee.jarsauth.pending.base.PendingList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class EventHandler {
    private static final Queue<ServerPlayer> kickList = new ArrayDeque<>();
    private static final Queue<Component> reasons = new ArrayDeque<>();
    private static final Queue<IntHolder> delayTicks = new ArrayDeque<>();

    private static final List<PendingList> pendingLists = new ArrayList<>();
    private static final DataManager dataManager = DataManager.getServerInstance();
    private static final ConfigService configService = dataManager.getService(ConfigService.class);

    private static DedicatedServer server;
    @SubscribeEvent
    public static void onServerStart(ServerStartedEvent event){
        MinecraftServer server = event.getServer();
        if(server instanceof DedicatedServer){
            ConfigService configService = DataManager.getServerInstance().getService(ConfigService.class);
            long fce = configService.getValue(ConfigKey.FILE_CHECKSUM_ENABLED);
            long cae = configService.getValue(ConfigKey.CLIENT_AUTH_ENABLED);
            long sle = configService.getValue(ConfigKey.SERVER_LICENSE_ENABLED);
            if(fce != 0){
                pendingLists.add(new FCPendingList(server));
            }
            if(cae != 0){
                pendingLists.add(new CAPendingList(server));
            }
            if(sle != 0){
                pendingLists.add(new SLPendingList(server));
            }
        }
        /*
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
        }*/
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        Player plr = event.getEntity();
        if(plr instanceof ServerPlayer svplr){
            pendingLists.forEach(pendingList -> pendingList.addUser(svplr.getUUID()));
            if(configService.getValue(ConfigKey.FILE_CHECKSUM_ENABLED) == 1){
                if(VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                    if(Objects.requireNonNull(svplr.getServer()).getPlayerCount() > 1){
                        svplr.connection.disconnect(Component.translatable("text.disconn.recording"));
                    }else{
                        ClientboundResourcePackPacket packet = new ClientboundResourcePackPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", false, Component.empty());
                        svplr.connection.send(packet);
                    }
                }
            }
            /*
            if(StaticConfig.getInstance().getBoolean(ConfigKey.FILE_CHECKSUM_ENABLED)){
                if(VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                    if(Objects.requireNonNull(svplr.getServer()).getPlayerCount() > 1){
                        svplr.connection.disconnect(Component.translatable("text.disconn.recording"));
                    }else{
                        ClientboundResourcePackPacket packet = new ClientboundResourcePackPacket("null", "JARSAUTH AUTHENTICATION INF0RMATION", false, Component.empty());
                        svplr.connection.send(packet);
                    }
                    //Compatibility.sendModPacket(svplr, packet);
                }else{
                    //FCPendingList.getInstance().playerLogin(svplr);
                }
            }

            if(Settings.getClientAuthSetting().isEnabled()){
                //CAPendingList.getInstance().playerLogin(svplr);
            }

            if(Settings.getServerLicenseSetting().isEnabled()){
                SLPendingList.getInstance().playerLogin(svplr);
            }

 */
        }
    }
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event){
        Player plr = event.getEntity();
        if(plr instanceof ServerPlayer svplr){
            pendingLists.forEach(pendingList -> pendingList.removeUser(svplr.getUUID()));
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

    public static void addPlayerToBeRemove(ServerPlayer player, Component reason, int delayTick){
        synchronized (kickList){
            kickList.add(player);
            reasons.add(reason);
            delayTicks.add(new IntHolder(delayTick));
        }
    }

    public static void reloadSettings(){
        /*
        Settings.loadAllSettings(serverSaveDir);
        Settings.printAll();
        LOGGER.info("Settings reloaded");
        */
    }

    public static void reloadDetails(){
        /*
        ClientDetail.reloadDetails(serverSaveDir);
        LOGGER.info("Client details reloaded");

         */
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

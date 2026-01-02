package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.pending.base.PendingList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;


public class FCPendingList extends PendingList {

    public FCPendingList(MinecraftServer server) {
        super(server, true);
    }

    @Override
    protected String calculateExpected(UUID userId, String random) throws Exception {
        return "test123";
    }

    @Override
    protected boolean compare(String expected, String actual) {
        return expected.equals(actual);
    }

    @Override
    protected void sendInfoToPlayer(UUID userId, String random) {
        ServerPlayer player = server.getPlayerList().getPlayer(userId);

        if (player == null) return;
        //player.connection.send(new ClientboundResourcePackPacket());
    }

    @Override
    protected void notifyFailure(UUID userId, String reason) {
        Component reasonComponent;
        if(reason.startsWith(".")){
            reasonComponent = Component.literal(reason.substring(1));
        }else{
            reasonComponent = Component.translatable(reason);
        }
        ServerPlayer player = server.getPlayerList().getPlayer(userId);
        EventHandler.addPlayerToBeRemove(player, reasonComponent, 0);
    }

    @Override
    protected String formatReason(FailureType type, Exception e) {
        if(e != null) {
            return "." + e.getMessage();
        }else{
            return "fc." + type.key();
        }
    }

    @Override
    protected long getInterval() {
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);
        return service.getValue(ConfigKey.FILE_CHECKSUM_INTERVAL);
    }

    @Override
    protected long getTimeout() {
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);
        return service.getValue(ConfigKey.FILE_CHECKSUM_TIMEOUT);
    }
}

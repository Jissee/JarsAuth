package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.model.ServerLicenseInstance;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.data.service.ServerLicenseService;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.pending.base.PendingList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class SLPendingList extends PendingList {
    public SLPendingList(MinecraftServer server) {
        super(server, false);
    }

    @Override
    protected String generateRandom() {
        return "";
    }

    @Override
    protected String calculateExpected(UUID userId, String random) throws Exception {
        Player player = server.getPlayerList().getPlayer(userId);
        if (player != null) {
            String playerName = player.getName().getString();
            ServerLicenseService sls = dataManager.getService(ServerLicenseService.class);
            boolean result = sls.updateAndVerifyForPlayer(playerName);
            return String.valueOf(result);
        }

        return "false";
    }

    @Override
    protected boolean compare(String expected, String actual) {
        return "true".equals(expected);
    }

    @Override
    protected void sendInfoToPlayer(UUID userId, String random) {
        // ignored
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
            return "sl." + type.key();
        }
    }

    @Override
    protected long getInterval() {
        ConfigService service = dataManager.getService(ConfigService.class);
        return service.getValue(ConfigKey.SERVER_LICENSE_INTERVAL);
    }

    @Override
    protected long getTimeout() {
        return 20;// this should never happen
    }
}

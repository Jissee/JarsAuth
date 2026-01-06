package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.pending.base.PendingList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.UUID;

public class CAPendingList extends PendingList {

    public CAPendingList(MinecraftServer server) {
        super(server, true);
    }

    @Override
    protected String calculateExpected(UUID userId, String random) throws Exception {
        return "";
    }

    @Override
    protected boolean compare(String expected, String actual) {
        return false;
    }

    @Override
    protected void sendInfoToPlayer(UUID userId, String random) {
        ServerPlayer player = server.getPlayerList().getPlayer(userId);
        if (player != null) {
            PlayerTeam team = new PlayerTeam(new Scoreboard(), "!!$%!$");
            ClientboundSetPlayerTeamPacket packet = ClientboundSetPlayerTeamPacket.createPlayerPacket(team,"", ClientboundSetPlayerTeamPacket.Action.ADD);


            //player.connection.send(new ClientboundSetPlayerTeamPacket());
        }
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
            return "ca." + type.key();
        }
    }

    @Override
    protected long getInterval() {
        ConfigService service = dataManager.getService(ConfigService.class);
        return service.getValue(ConfigKey.CLIENT_AUTH_INTERVAL);
    }

    @Override
    protected long getTimeout() {
        ConfigService service = dataManager.getService(ConfigService.class);
        return service.getValue(ConfigKey.CLIENT_AUTH_TIMEOUT);
    }
}

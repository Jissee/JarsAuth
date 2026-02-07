package me.jissee.jarsauth.pending;

import me.jissee.jarsauth.Codec;
import me.jissee.jarsauth.DisconnectionHandler;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.data.service.ServerIdService;
import me.jissee.jarsauth.data.service.UserIdServerService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class CAPendingList extends AbstractPendingList<byte[]> {

    public CAPendingList(MinecraftServer server) {
        super(server, true);
    }

    @Override
    protected byte[] calculateExpected(UUID userId, String random){
        ServerPlayer player = server.getPlayerList().getPlayer(userId);
        if(player == null){
            return new byte[0];
        }
        String playerName = player.getName().getString();
        UserIdServerService service = dataManager.getService(UserIdServerService.class);
        if(service.hasUserId(playerName)){
            UUID uuid = service.getOrCreateUserId(playerName).get();
            return Codec.uuidToBytes(uuid);
        }else{
            return new byte[0];
        }
    }

    @Override
    protected boolean compare(UserContext ctx, byte[] expected, byte[] actualEnc) {
        PublicKey publicKey = ctx.key;
        PrivateKey privateKey = Codec.getPrivateKey(publicKey);
        try {
            byte[] byteUuid = Codec.decrypt(actualEnc, privateKey);
            return Arrays.compare(expected, byteUuid) == 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendInfoToPlayer(UserContext ctx, String random) {
        ServerPlayer player = server.getPlayerList().getPlayer(ctx.userId);

        if (player != null) {
            UserIdServerService userIdServerService = dataManager.getService(UserIdServerService.class);
            ServerIdService serverIdService = dataManager.getService(ServerIdService.class);
            UUID serverId = serverIdService.getOrCreateServerId();

            String playerName = player.getName().getString();
            boolean hasUuid = userIdServerService.hasUserId(playerName);


            PlayerTeam team;
            Collection<String> payload;

            int flag;
            if (hasUuid) { // auth
                flag = -0514;
                team = new PlayerTeam(new Scoreboard(), "!!$%!$" + flag);

                String serverPk;
                try {
                    PublicKey key = Codec.getKey();
                    ctx.key = key;
                    byte[] keyArray = key.getEncoded();
                    serverPk = Codec.bytesToHex(keyArray);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                payload = team.getPlayers();
                payload.add("<" + serverPk + ">");
            }else{ // register
                flag = -0114;
                team = new PlayerTeam(new Scoreboard(), "!!$%!$" + flag);
                payload = team.getPlayers();
            }
            payload.add("[" + serverId + "]");

            ClientboundSetPlayerTeamPacket packet =
                    ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
            player.connection.send(packet);
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
        DisconnectionHandler.addPlayerToBeRemove(player, reasonComponent, 0);
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

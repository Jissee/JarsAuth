package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.config.VolatileConfig;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.pending.AbstractPendingList;
import me.jissee.jarsauth.pending.CAPendingList;
import me.jissee.jarsauth.pending.FCPendingList;
import me.jissee.jarsauth.pending.SLPendingList;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    public void onPlayerLogin(Connection p_11262_, ServerPlayer p_11263_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(FMLLoader.getDist() == Dist.DEDICATED_SERVER){
            ServerPlayer svplr = p_11263_;
            ConfigService configService = DataManager.getServerInstance().getService(ConfigService.class);
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
                    AbstractPendingList.get(FCPendingList.class).addUser(svplr.getUUID());
                }
            }
            if(configService.getValue(ConfigKey.CLIENT_AUTH_ENABLED) == 1){
                AbstractPendingList.get(CAPendingList.class).addUser(svplr.getUUID());
            }
            if(configService.getValue(ConfigKey.SERVER_LICENSE_ENABLED) == 1){
                AbstractPendingList.get(SLPendingList.class).addUser(svplr.getUUID());
            }

        }
    }
    @Inject(method = "remove", at = @At("HEAD"))
    public void onPlayerLogout(ServerPlayer p_11287_, CallbackInfo ci) {
        if (Assert.assertFalse(true)) return;
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            ServerPlayer svplr = p_11287_;
            AbstractPendingList.forEach(pendingList -> pendingList.removeUser(svplr.getUUID()));
        }
    }

}

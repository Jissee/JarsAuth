/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.Codec;
import me.jissee.jarsauth.config.VolatileConfig;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.AccProfileService;
import me.jissee.jarsauth.data.service.ServerIdService;
import me.jissee.jarsauth.data.service.UserIdServerService;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.pending.CAPendingList;
import me.jissee.jarsauth.pending.FCPendingList;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(value = ServerboundEditBookPacket.class, priority = 0)
public class ServerboundPacketMixin {
    private static final int replaceTarget1 = -1919; // fc auth
    private static final int replaceTarget2 = -810;  // fc send info
    private static final int replaceTarget3 = -191;  // ca auth
    private static final int replaceTarget4 = -9810; // ca receive pk

    private int receivedCount = 0;
    @Shadow @Final
    private List<String> pages;

    @Shadow @Final
    private int slot;

    @Shadow @Final private Optional<String> title;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerboundPacketMixin.class);

    @Inject(method = {"handle(Lnet/minecraft/network/protocol/game/ServerGamePacketListener;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void inj1(ServerGamePacketListener p_134008_, CallbackInfo ci) throws Exception {
        if(Assert.assertFalse(true)) return;
        ServerGamePacketListenerImpl er = (ServerGamePacketListenerImpl) p_134008_;
        MinecraftServer server = er.player.getServer();
        boolean isDefault = false;
        if(server instanceof DedicatedServer){
            switch (this.slot){
                case replaceTarget1:
                    FCPendingList pending = EventHandler.getPendingList(FCPendingList.class);
                    pending.onVerificationResponse(er.player.getUUID(), pages);
                    break;
                case replaceTarget2:
                    if(VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                        Thread thread = new Thread(()->{
                            assert title.isPresent();
                            int totalCount = Integer.parseInt(title.get());
                            LOGGER.debug("received packet {}/{}", receivedCount, pages);
                            for(int i = 0; i < pages.size(); i += 2){
                                String key = pages.get(i);
                                String value = pages.get(i + 1);
                                AccProfileService service = DataManager.getServerInstance().getService(AccProfileService.class);
                                if(service.buffer(key, value, totalCount)){
                                    EventHandler.addPlayerToBeRemove(er.player, Component.translatable("text.disconn.recorded"), 0);
                                    receivedCount = 0;
                                }
                            }
                        });
                        thread.start();
                        ci.cancel();
                    }
                    break;
                case replaceTarget3:
                    CAPendingList pendingList = EventHandler.getPendingList(CAPendingList.class);
                    if(!pages.isEmpty()){
                        String uuid = pages.get(0);
                        pendingList.onVerificationResponse(er.player.getUUID(), Codec.hexToBytes(uuid));
                    }
                    break;
                case replaceTarget4:
                    boolean success = false;
                    if(!pages.isEmpty()){
                        String clientPkStr = pages.get(0);

                        PublicKey clientPk = Codec.byteArr2PublicKey(Codec.hexToBytes(clientPkStr));
                        UserIdServerService service = DataManager.getServerInstance().getService(UserIdServerService.class);
                        ServerIdService serverIdService = DataManager.getServerInstance().getService(ServerIdService.class);
                        Optional<UUID> userId = service.getOrCreateUserId(er.player.getName().getString());
                        if(userId.isPresent()){
                            UUID uuid = userId.get();
                            byte[] enc = Codec.encrypt(Codec.uuidToBytes(uuid), clientPk);
                            int flag = -0114;
                            PlayerTeam team = new PlayerTeam(new Scoreboard(), "!!$%!$" + flag);
                            Collection<String> payload = team.getPlayers();
                            payload.add(Codec.bytesToHex(enc));
                            payload.add("[" + serverIdService.getOrCreateServerId() + "]");
                            ClientboundSetPlayerTeamPacket packet =
                                    ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
                            er.player.connection.send(packet);
                            success = true;
                        }else {
                            LOGGER.error("Could not create user id for {}", er.player.getName().getString());
                        }

                    }
                    Component reason = Component.translatable("text.disconn.first.time");
                    if(!success){
                        reason = Component.translatable("text.disconn.ca.error");
                    }
                    EventHandler.addPlayerToBeRemove(er.player, reason, 0);
                    break;
                default:
                    isDefault = true;
            }
            if(!isDefault){
                ci.cancel();
            }
        }
    }
}

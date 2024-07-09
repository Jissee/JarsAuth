/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import io.netty.buffer.ByteBuf;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.client_auth.CAPendingList;
import me.jissee.jarsauth.client_auth.Codec;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.server_settings.Settings;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

import static me.jissee.jarsauth.JarsAuth.MODID;

public record CAAuthPacket(String serverID, int authType, byte[] payload) implements CustomPacketPayload {
    public static final int PUBLIC_KEY = 314159265;
    public static final int AUTH_INFO = 271828172;

    public static final Type<CAAuthPacket> TYPE = new Type<>(ResourceLocation.tryBuild(MODID, "ca_auth"));


    public static final StreamCodec<ByteBuf, CAAuthPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CAAuthPacket::serverID,
            ByteBufCodecs.INT,
            CAAuthPacket::authType,
            ByteBufCodecs.BYTE_ARRAY,
            CAAuthPacket::payload,
            CAAuthPacket::new
    );

    public static void handle(CAAuthPacket packet0, IPayloadContext ctx){
        ctx.enqueueWork(()->{
            if(!Settings.getServerID().getServerID().equals(packet0.serverID)){
                return;
            }
            switch (packet0.authType){
                case PUBLIC_KEY:
                    try{
                        byte[] pub = packet0.payload;

                        PublicKey publicKey = Codec.byteArr2PublicKey(pub);
                        ServerPlayer plr = (ServerPlayer) ctx.player();
                        if(plr == null){
                            return;
                        }
                        Optional<UUID> playerUUID = CAPendingList.getInstance().getWaitingPlayerUUID(plr.getName().getString());
                        if(playerUUID.isEmpty()){
                            return;
                        }
                        UUID uuid = playerUUID.get();
                        byte[] cipher = Codec.encrypt(Codec.uuidToBytes(uuid), publicKey);
                        if(cipher.length == 0){
                            return;
                        }
                        CABroadcastPacket packet = new CABroadcastPacket(Settings.getServerID().getServerID(), CABroadcastPacket.AUTH_INFO, cipher);

                        Compatibility.sendModPacket(plr, packet);
                        EventHandler.addPlayerToBeRemove(plr, Compatibility.translatable("text.firsttime"), 10);
                    }catch(Exception e){
                        throw new RuntimeException();
                    }
                    break;
                case AUTH_INFO:
                    ServerPlayer plr = (ServerPlayer) ctx.player();
                    if(plr != null){
                        CAPendingList.getInstance().addAuthInfo(packet0.serverID, plr, packet0.payload);
                    }
                    break;
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event){
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TYPE, STREAM_CODEC, CAAuthPacket::handle);
    }
}

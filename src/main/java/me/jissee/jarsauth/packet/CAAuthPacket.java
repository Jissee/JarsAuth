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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

import static me.jissee.jarsauth.JarsAuth.MODID;

public record CAAuthPacket(String serverID, int authType, byte[] payload) implements CustomPayload {
    public static final int PUBLIC_KEY = 314159265;
    public static final int AUTH_INFO = 271828172;

    public static final Id<CAAuthPacket> TYPE = new Id<>(Identifier.of(MODID, "ca_auth"));


    public static final PacketCodec<ByteBuf, CAAuthPacket> STREAM_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            CAAuthPacket::serverID,
            PacketCodecs.INTEGER,
            CAAuthPacket::authType,
            PacketCodecs.BYTE_ARRAY,
            CAAuthPacket::payload,
            CAAuthPacket::new
    );

    public static void handle(CAAuthPacket packet0, ServerPlayNetworking.Context ctx){
        //ctx.server().execute(()->{
            if(!Settings.getServerID().getServerID().equals(packet0.serverID)){
                return;
            }
            switch (packet0.authType){
                case PUBLIC_KEY:
                    try{
                        byte[] pub = packet0.payload;

                        PublicKey publicKey = Codec.byteArr2PublicKey(pub);
                        ServerPlayerEntity plr = (ServerPlayerEntity) ctx.player();
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
                    ServerPlayerEntity plr = (ServerPlayerEntity) ctx.player();
                    if(plr != null){
                        CAPendingList.getInstance().addAuthInfo(packet0.serverID, plr, packet0.payload);
                    }
                    break;
            }
        //});
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void registerCodec(){
        PayloadTypeRegistry.playC2S().register(TYPE, STREAM_CODEC);
    }
    public static void registerHandler(){
        ServerPlayNetworking.registerGlobalReceiver(TYPE, CAAuthPacket::handle);
    }
}

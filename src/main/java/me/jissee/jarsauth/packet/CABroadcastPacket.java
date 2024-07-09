/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import io.netty.buffer.ByteBuf;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.client_auth.Codec;
import me.jissee.jarsauth.client_auth.DataUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

import static me.jissee.jarsauth.JarsAuth.MODID;

public record CABroadcastPacket(String serverID, int authType, byte[] payload) implements CustomPayload {
    public static final int PUBLIC_KEY = 271827182;
    public static final int AUTH_INFO = 314159265;

    public static final Id<CABroadcastPacket> TYPE = new Id<>(Identifier.of(MODID, "ca_broadcast"));


    public static final PacketCodec<ByteBuf, CABroadcastPacket> STREAM_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            CABroadcastPacket::serverID,
            PacketCodecs.INTEGER,
            CABroadcastPacket::authType,
            PacketCodecs.BYTE_ARRAY,
            CABroadcastPacket::payload,
            CABroadcastPacket::new
    );

    private static PrivateKey pri;


    public static void handle(CABroadcastPacket packet0, ClientPlayNetworking.Context ctx){
        //ctx.client().execute(()->{
            if(packet0.payload.length == 0){//register new
                KeyPair pair;
                try {
                    pair = Codec.generateKeyPair();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                pri = pair.getPrivate();
                PublicKey pub = pair.getPublic();

                CAAuthPacket packet = new CAAuthPacket(
                        packet0.serverID,
                        CAAuthPacket.PUBLIC_KEY,
                        pub.getEncoded()
                );

                PacketHandler.sendToServerSender(ctx.responseSender(), packet);
            }else{//send auth info
                switch (packet0.authType){
                    case AUTH_INFO://decrypt
                        try {
                            byte[] uuidEnc = packet0.payload;
                            byte[] decrypted = Codec.decrypt(uuidEnc, pri);
                            pri = null;
                            UUID uuid = Codec.bytesToUUID(decrypted);
                            String name = Compatibility.getStringName();
                            DataUtil.saveUUID(packet0.serverID, name, uuid);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case PUBLIC_KEY :
                        try {
                            String name = Compatibility.getStringName();
                            Optional<UUID> uuid = DataUtil.readUUID(packet0.serverID, name);
                            if(uuid.isEmpty()){
                                return;
                            }
                            byte[] uuidByteOrigin = Codec.uuidToBytes(uuid.get());
                            byte[] pub = packet0.payload;
                            PublicKey publicKey = Codec.byteArr2PublicKey(pub);
                            byte[] encrypted = Codec.encrypt(uuidByteOrigin, publicKey);
                            CAAuthPacket packet = new CAAuthPacket(packet0.serverID, CAAuthPacket.AUTH_INFO, encrypted);
                            PacketHandler.sendToServerSender(ctx.responseSender(), packet);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        break;
                }
            }
        //});
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }
    public static void registerCodec(){
        PayloadTypeRegistry.playS2C().register(TYPE, STREAM_CODEC);
    }
    public static void registerHandler(){
        ClientPlayNetworking.registerGlobalReceiver(TYPE, CABroadcastPacket::handle);
    }
}

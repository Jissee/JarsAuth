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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

import static me.jissee.jarsauth.JarsAuth.MODID;

public record CABroadcastPacket(String serverID, int authType, byte[] payload) implements CustomPacketPayload {
    public static final int PUBLIC_KEY = 271827182;
    public static final int AUTH_INFO = 314159265;

    public static final Type<CABroadcastPacket> TYPE = new Type<>(ResourceLocation.tryBuild(MODID, "ca_broadcast"));


    public static final StreamCodec<ByteBuf, CABroadcastPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            CABroadcastPacket::serverID,
            ByteBufCodecs.INT,
            CABroadcastPacket::authType,
            ByteBufCodecs.BYTE_ARRAY,
            CABroadcastPacket::payload,
            CABroadcastPacket::new
    );

    private static PrivateKey pri;


    public static void handle(CABroadcastPacket packet0, IPayloadContext ctx){
        ctx.enqueueWork(()->{
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

                PacketHandler.sendToServer(packet);
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
                            PacketHandler.sendToServer(packet);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        break;
                }
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
        registrar.playToClient(TYPE, STREAM_CODEC, CABroadcastPacket::handle);
    }
}

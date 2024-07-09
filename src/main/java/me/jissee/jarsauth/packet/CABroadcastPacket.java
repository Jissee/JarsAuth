/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.client_auth.Codec;
import me.jissee.jarsauth.client_auth.DataUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

import static me.jissee.jarsauth.JarsAuth.MODID;
import static me.jissee.jarsauth.packet.CAAuthPacket.CA_AUTH_PACKET;


public class CABroadcastPacket implements ModPacket {
    public static final Identifier CA_BROADCAST_PACKET = new Identifier(MODID, "ca_broadcast_packet");
    public static final int PUBLIC_KEY = 271827182;
    public static final int AUTH_INFO = 314159265;

    private static PrivateKey pri;

    private final String serverID;
    private final int type;
    private final byte[] payload;

    public CABroadcastPacket(String serverID, int type, byte[] payload) {
        this.serverID = serverID;
        this.type = type;
        this.payload = payload;
    }


    @Override
    public Identifier getType() {
        return CA_BROADCAST_PACKET;
    }

    public void encode(PacketByteBuf buf){
        buf.writeString(this.serverID);
        buf.writeInt(type);
        buf.writeByteArray(payload);
    }

    public static CABroadcastPacket decode(PacketByteBuf buf){
        return new CABroadcastPacket(buf.readString(), buf.readInt(), buf.readByteArray());
    }
    public static void onClientReceive(
            MinecraftClient client,
            ClientPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender sender
    ) {
        CABroadcastPacket packet0 = decode(buf);


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

            PacketByteBuf buf1 = PacketByteBufs.create();
            packet.encode(buf1);
            sender.sendPacket(packet.getType(), buf1);
        }else{//send auth info
            switch (packet0.type){
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
                        PacketByteBuf buf1 = PacketByteBufs.create();
                        new CAAuthPacket(packet0.serverID, CAAuthPacket.AUTH_INFO, encrypted).encode(buf1);
                        sender.sendPacket(CA_AUTH_PACKET, buf1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }

    }


}

/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.client_auth.CAPendingList;
import me.jissee.jarsauth.client_auth.Codec;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.server_settings.Settings;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;

import static me.jissee.jarsauth.JarsAuth.MODID;


public class CAAuthPacket implements ModPacket {
    public static final Identifier CA_AUTH_PACKET = new Identifier(MODID, "ca_auth_packet");
    public static final int PUBLIC_KEY = 314159265;
    public static final int AUTH_INFO = 271828172;

    private final String serverID;
    private final int type;
    private final byte[] payload;

    public CAAuthPacket(String serverID, int type, byte[] payload){
        this.serverID = serverID;
        this.type = type;
        this.payload = payload;
    }

    @Override
    public Identifier getType() {
        return CA_AUTH_PACKET;
    }

    public void encode(PacketByteBuf buf){
        buf.writeString(serverID);
        buf.writeInt(type);
        buf.writeByteArray(payload);
    }
    public static CAAuthPacket decode(PacketByteBuf buf){
        return new CAAuthPacket(buf.readString(), buf.readInt(), buf.readByteArray());
    }
    public static void onServerReceive(
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender sender
    ){
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            CAAuthPacket packet0 = decode(buf);


            if(!Settings.getServerID().getServerID().equals(packet0.serverID)){
                return;
            }
            switch (packet0.type){
                case PUBLIC_KEY:
                    try{
                        byte[] pub = packet0.payload;

                        PublicKey publicKey = Codec.byteArr2PublicKey(pub);
                        ServerPlayerEntity plr = player;
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

                        Compatibility.sendModPacket(sender, packet);
                        EventHandler.addPlayerToBeRemove(plr, Compatibility.translatable("text.firsttime"), 10);
                    }catch(Exception e){
                        throw new RuntimeException();
                    }
                    break;
                case AUTH_INFO:
                    ServerPlayerEntity plr = player;
                    if(plr != null){
                        CAPendingList.getInstance().addAuthInfo(packet0.serverID, plr, packet0.payload);
                    }
                    break;
            }
        }
    }


}

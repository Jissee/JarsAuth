/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PacketHandler {
    public static void registerPacketsHandledByServer(){
        ServerPlayNetworking.registerGlobalReceiver(FCAuthPacket.FC_AUTH_PACKET, FCAuthPacket::onServerReceive);
        ServerPlayNetworking.registerGlobalReceiver(CAAuthPacket.CA_AUTH_PACKET, CAAuthPacket::onServerReceive);
    }

    public static void registerPacketsHandledByClient(){
        ClientPlayNetworking.registerGlobalReceiver(FCBroadcastPacket.FC_BROADCAST_PACKET, FCBroadcastPacket::onClientReceive);
        ClientPlayNetworking.registerGlobalReceiver(CABroadcastPacket.CA_BROADCAST_PACKET, CABroadcastPacket::onClientReceive);
    }

    public static <T extends PacketByteBuf> void sendToPlayer(Identifier channel, T packetBuf, ServerPlayerEntity svplr){
        ServerPlayNetworking.send(svplr, channel, packetBuf);
    }
}

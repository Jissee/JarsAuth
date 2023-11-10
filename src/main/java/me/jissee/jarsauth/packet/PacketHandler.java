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
        ServerPlayNetworking.registerGlobalReceiver(AuthPacket.AUTH_PACKET, AuthPacket::onServerReceive);
    }

    public static void registerPacketsHandledByClient(){
        ClientPlayNetworking.registerGlobalReceiver(BroadcastPacket.BROADCAST_PACKET, BroadcastPacket::onClientReceive);
    }

    public static <T extends PacketByteBuf> void sendToPlayer(Identifier channel, T packetBuf, ServerPlayerEntity svplr){
        ServerPlayNetworking.send(svplr, channel, packetBuf);
    }
}

/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.packet.CustomPayload;

public class PacketHandler {
    public static void registerAllCodec(){
        CAAuthPacket.registerCodec();
        FCAuthPacket.registerCodec();
        CABroadcastPacket.registerCodec();
        FCBroadcastPacket.registerCodec();
    }
    public static void registerPacketsHandledByServer(){
        CAAuthPacket.registerHandler();
        FCAuthPacket.registerHandler();
    }

    public static void registerPacketsHandledByClient(){
        CABroadcastPacket.registerHandler();
        FCBroadcastPacket.registerHandler();
    }

    public static void sendToServerSender(PacketSender sender, CustomPayload packet){
        sender.sendPacket(packet);
    }
}

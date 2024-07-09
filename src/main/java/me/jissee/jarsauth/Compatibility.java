/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth;

import com.mojang.brigadier.context.CommandContext;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.packet.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;

public class Compatibility {
    public static void disconnectWithReason(ServerPlayer player, Component reason) {
        if(!player.hasDisconnected()){
            player.connection.disconnect(reason);
        }
    }
    public static void sendVanillaPacket(ServerPlayer player, Packet<?> packet){
        player.connection.send(packet);
    }
    public static void sendModPacket(ServerPlayer player, CustomPacketPayload packet){
        if(!player.hasDisconnected()){
            PacketHandler.sendToPlayer(packet, player);
        }
    }
    public static boolean hasDisconnected(ServerPlayer player){
        return player.hasDisconnected();
    }
    public static String getClientRootDir(){
        return Minecraft.getInstance().gameDirectory.getAbsolutePath() + File.separator;
    }
    public static ServerPlayer getPlayerByName(String name){
        return EventHandler.getServer().getPlayerList().getPlayerByName(name);
    }
    public static Component translatable(String key){
        return Component.translatable(key);
    }
    public static Component literal(String text){
        return Component.literal(text);
    }
    public static String getStringName() {
        return Minecraft.getInstance().player.getName().getString();
    }
    public static void consoleMessage(CommandContext<CommandSourceStack> ctx, Component msg){
        ctx.getSource().sendSystemMessage(msg);
    }
}

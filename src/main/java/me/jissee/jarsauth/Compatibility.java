/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth;

import com.mojang.brigadier.context.CommandContext;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.packet.ModPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Compatibility {
    private static Map<String, PacketSender> playerSender = new HashMap<>();
    public static void addPlayerSender(String player, PacketSender sender){
        playerSender.put(player, sender);
    }
    public static void disconnectWithReason(ServerPlayerEntity player, Text reason) {
        if(!player.isDisconnected()){
            player.networkHandler.disconnect(reason);
        }
    }
    public static void sendVanillaPacket(ServerPlayerEntity player, Packet<?> packet){
        player.networkHandler.sendPacket(packet);
    }
    public static void sendModPacket(ServerPlayerEntity player, ModPacket packet){
        if(player != null && !player.isDisconnected() && packet != null){
            PacketByteBuf buf = PacketByteBufs.create();
            packet.encode(buf);
            PacketHandler.sendToPlayer(packet.getType(), buf, player);
        }
    }
    public static void sendModPacket(PacketSender sender, ModPacket packet){
        if(sender != null && packet != null){
            PacketByteBuf buf = PacketByteBufs.create();
            packet.encode(buf);
            sender.sendPacket(packet.getType(), buf);
        }
    }
    public static boolean hasDisconnected(ServerPlayerEntity player){
        return player.isDisconnected();
    }
    public static String getClientRootDir(){
        return MinecraftClient.getInstance().runDirectory.getAbsolutePath() + File.separator;
    }
    //public static void sendToServer(Object packet){
    //    PacketHandler.sendToServer(packet);
    //}
    public static ServerPlayerEntity getPlayerByName(String name){
        return EventHandler.getServer().getPlayerManager().getPlayer(name);
    }
    public static Text translatable(String key){
        return Text.translatable(key);
    }
    public static Text literal(String text){
        return Text.literal(text);
    }
    public static String getStringName() {
        return MinecraftClient.getInstance().player.getName().getString();
    }
    public static void consoleMessage(CommandContext<ServerCommandSource> ctx, Text msg){
        ctx.getSource().sendMessage(msg);
    }
}

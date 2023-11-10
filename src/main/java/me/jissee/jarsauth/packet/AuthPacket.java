/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.profile.ClientDetail;
import me.jissee.jarsauth.util.PendingList;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

import static me.jissee.jarsauth.JarsAuth.MODID;


public class AuthPacket {
    public static final Identifier AUTH_PACKET = new Identifier(MODID, "auth_packet");
    private final int slot;
    private final List<String> pages;

    public AuthPacket(int slot, List<String> pages){
        this.slot = slot;
        this.pages = ImmutableList.copyOf(pages);
    }
    public AuthPacket(PacketByteBuf buf){
        this.slot = buf.readVarInt();
        this.pages = buf.readCollection(PacketByteBuf.getMaxValidator(Lists::newArrayListWithCapacity, 200), buf2 -> buf2.readString(8192));
    }
    public void encode(PacketByteBuf buf){
        buf.writeVarInt(this.slot);
        buf.writeCollection(this.pages, (buf2, page) -> buf2.writeString((String)page, 8192));
    }
    public static AuthPacket decode(PacketByteBuf buf){
        return new AuthPacket(buf);
    }
    public static void onServerReceive(
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender sender
    ){
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            AuthPacket packet = new AuthPacket(buf);
            if(packet.slot == -114514){
                PendingList.getInstance().addHash2(player, packet.pages.get(0));
            }else if(packet.slot == 114514 && EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                Thread thread = new Thread(()->{
                    for(int i = 0; i < packet.pages.size(); i += 2){
                        String key = packet.pages.get(i);
                        String value = packet.pages.get(i + 1);
                        ClientDetail.add(key, value);
                    }
                });
                thread.start();
            }
        }
    }


}

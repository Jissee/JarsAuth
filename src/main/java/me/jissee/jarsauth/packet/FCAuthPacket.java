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
import me.jissee.jarsauth.file_checksum.FCPendingList;
import me.jissee.jarsauth.server_settings.ClientDetail;
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


public class FCAuthPacket implements ModPacket{
    public static final Identifier FC_AUTH_PACKET = new Identifier(MODID, "fc_auth_packet");
    private final int slot;
    private final List<String> pages;

    public FCAuthPacket(int slot, List<String> pages){
        this.slot = slot;
        this.pages = ImmutableList.copyOf(pages);
    }

    @Override
    public Identifier getType() {
        return FC_AUTH_PACKET;
    }

    public void encode(PacketByteBuf buf){
        buf.writeVarInt(this.slot);
        buf.writeCollection(this.pages, (buf2, page) -> buf2.writeString((String)page, 8192));
    }
    public static FCAuthPacket decode(PacketByteBuf buf){
        return new FCAuthPacket(
                buf.readVarInt(),
                buf.readCollection(PacketByteBuf.getMaxValidator(Lists::newArrayListWithCapacity, 200), buf2 -> buf2.readString(8192))
        );
    }
    public static void onServerReceive(
            MinecraftServer server,
            ServerPlayerEntity player,
            ServerPlayNetworkHandler handler,
            PacketByteBuf buf,
            PacketSender sender
    ){
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            FCAuthPacket packet0 = decode(buf);

            if(packet0.slot == -114514){
                FCPendingList.getInstance().addHash2(player, packet0.pages.get(0));
            }else if(packet0.slot == 114514 && EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                Thread thread = new Thread(()->{
                    for(int i = 0; i < packet0.pages.size(); i += 2){
                        String key = packet0.pages.get(i);
                        String value = packet0.pages.get(i + 1);
                        ClientDetail.add(key, value);
                    }
                });
                thread.start();
            }
        }
    }


}

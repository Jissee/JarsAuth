/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import io.netty.buffer.ByteBuf;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.file_checksum.FCPendingList;
import me.jissee.jarsauth.server_settings.ClientDetail;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

import static me.jissee.jarsauth.JarsAuth.MODID;


public record FCAuthPacket(int slot, List<String> pages) implements CustomPayload {

    public static final Id<FCAuthPacket> TYPE = new Id<>(Identifier.of(MODID, "fc_auth"));
    public static final PacketCodec<ByteBuf, FCAuthPacket> STREAM_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            FCAuthPacket::slot,
            PacketCodecs.STRING.collect(PacketCodecs.toList()),
            FCAuthPacket::pages,
            FCAuthPacket::new
    );
    public static void handle(FCAuthPacket packet0, ServerPlayNetworking.Context ctx){
        //ctx.server().execute(()->{
            if(Objects.requireNonNull(ctx.player()).getServer() instanceof DedicatedServer){
                if(packet0.slot == -114514){
                    FCPendingList.getInstance().addHash2((ServerPlayerEntity) ctx.player(), packet0.pages.get(0));
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
        //});

    }


    @Override
    public Id<? extends CustomPayload> getId() {
        return TYPE;
    }

    public static void registerCodec(){
        PayloadTypeRegistry.playC2S().register(TYPE, STREAM_CODEC);
    }
    public static void registerHandler(){
        ServerPlayNetworking.registerGlobalReceiver(TYPE, FCAuthPacket::handle);
    }
}

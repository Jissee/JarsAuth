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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Objects;

import static me.jissee.jarsauth.JarsAuth.MODID;


public record FCAuthPacket(int slot, List<String> pages) implements CustomPacketPayload {

    public static final Type<FCAuthPacket> TYPE = new Type<>(ResourceLocation.tryBuild(MODID, "fc_auth"));
    public static final StreamCodec<ByteBuf, FCAuthPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            FCAuthPacket::slot,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            FCAuthPacket::pages,
            FCAuthPacket::new
    );
    public static void handle(FCAuthPacket packet0, IPayloadContext ctx){
        ctx.enqueueWork(()->{
            if(Objects.requireNonNull(ctx.player()).getServer() instanceof DedicatedServer){
                if(packet0.slot == -114514){
                    FCPendingList.getInstance().addHash2((ServerPlayer) ctx.player(), packet0.pages.get(0));
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
        });

    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event){
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TYPE, STREAM_CODEC, FCAuthPacket::handle);
    }
}

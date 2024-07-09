/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.packet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.file_checksum.FCPendingList;
import me.jissee.jarsauth.server_settings.ClientDetail;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;


public class FCAuthPacket {
    private final int slot;
    private final List<String> pages;

    public FCAuthPacket(int slot, List<String> pages){
        this.slot = slot;
        this.pages = ImmutableList.copyOf(pages);
    }
    public FCAuthPacket(FriendlyByteBuf buf){
        this.slot = buf.readInt();
        this.pages = buf.readCollection(FriendlyByteBuf.limitValue(Lists::newArrayListWithCapacity, 10000), (p_182763_) -> {
            return p_182763_.readUtf(8192);
        });
    }
    public void encode(FriendlyByteBuf buf){
        buf.writeInt(slot);
        buf.writeCollection(this.pages, (p_182759_, p_182760_) -> {
            p_182759_.writeUtf(p_182760_, 8192);
        });
    }
    public static FCAuthPacket decode(FriendlyByteBuf buf){
        return new FCAuthPacket(buf);
    }
    public boolean handle(Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{
            if(Objects.requireNonNull(ctx.get().getSender()).getServer() instanceof DedicatedServer){
                if(this.slot == -114514){
                    FCPendingList.getInstance().addHash2(ctx.get().getSender(), pages.get(0));
                }else if(this.slot == 114514 && EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                    Thread thread = new Thread(()->{
                        for(int i = 0; i < pages.size(); i += 2){
                            String key = pages.get(i);
                            String value = pages.get(i + 1);
                            ClientDetail.add(key, value);
                        }
                    });
                    thread.start();
                }
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }


}

/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.mixin;

import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.util.PendingList;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ServerboundEditBookPacket.class, priority = 0)
public class ServerboundPacket {
    @Shadow @Final
    private List<String> pages;
    @Shadow @Final
    private int slot;
    @Inject(method = {"handle(Lnet/minecraft/network/protocol/game/ServerGamePacketListener;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void inj1(ServerGamePacketListener p_134008_, CallbackInfo ci){
        ServerGamePacketListenerImpl er = (ServerGamePacketListenerImpl) p_134008_;
        MinecraftServer server = er.player.getServer();
        if(server instanceof DedicatedServer){
            if(this.slot == -114514){
                PendingList.getInstance().addHash1(er.player, pages.get(0));
                ci.cancel();
            }
        }
    }
}

/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.util.PendingList;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BookUpdateC2SPacket.class, priority = 0)
public class ServerboundPacket {
    @Shadow @Final
    private List<String> pages;
    @Shadow @Final
    private int slot;
    @Inject(method = {"apply(Lnet/minecraft/network/listener/ServerPlayPacketListener;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void inj1(ServerPlayPacketListener serverPlayPacketListener, CallbackInfo ci){
        ServerPlayNetworkHandler er = (ServerPlayNetworkHandler) serverPlayPacketListener;
        MinecraftServer server = er.player.getServer();
        if(server instanceof DedicatedServer){
            if(this.slot == -114514){
                PendingList.getInstance().addHash1(er.player, pages.get(0));
                ci.cancel();
            }
        }
    }
}

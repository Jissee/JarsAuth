/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.config.VolatileConfig;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.AccProfileService;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(value = ServerboundEditBookPacket.class, priority = 0)
public class ServerboundPacketMixin {
    private static final int replaceTarget1 = -1919; // fc auth
    private static final int replaceTarget2 = -810;  // fc send info
    private static final int replaceTarget3 = -1919810;  // ca auth

    private int receivedCount = 0;
    @Shadow @Final
    private List<String> pages;
    @Shadow @Final
    private int slot;

    @Shadow @Final private Optional<String> title;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerboundPacketMixin.class);

    @Inject(method = {"handle(Lnet/minecraft/network/protocol/game/ServerGamePacketListener;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void inj1(ServerGamePacketListener p_134008_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        ServerGamePacketListenerImpl er = (ServerGamePacketListenerImpl) p_134008_;
        MinecraftServer server = er.player.getServer();
        if(server instanceof DedicatedServer){
            switch (this.slot){

            }



            if(this.slot == replaceTarget1){
                //FCPendingList.getInstance().addHash2(ctx.get().getSender(), pages.get(0));
                ci.cancel();
            }else if(this.slot == replaceTarget2 && VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                Thread thread = new Thread(()->{
                    assert title.isPresent();
                    int totalCount = Integer.parseInt(title.get());
                    LOGGER.debug("received packet {}/{}", receivedCount, pages);
                    for(int i = 0; i < pages.size(); i += 2){
                        String key = pages.get(i);
                        String value = pages.get(i + 1);
                        AccProfileService service = DataManager.getServerInstance().getService(AccProfileService.class);
                        if(service.buffer(key, value, totalCount)){
                            er.player.connection.disconnect(Component.translatable("text.disconn.recorded"));
                            receivedCount = 0;
                        }
                    }
                });
                thread.start();
                ci.cancel();
            }
        }
    }
}

package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ClientboundSetPlayerTeamPacket.class)
public class ClientboundPacketMixin {
    private static final int replaceTarget1 = -114;  // fa record
    private static final int replaceTarget2 = -514;// fa auth info
    private static final int replaceTarget3 = -0114;  // ca new id
    private static final int replaceTarget4 = -0514;  // fa sending auth
    private static final int replaceTarget5 = -114514;// fa sending archive


    @Shadow @Final private Collection<String> players;
    @Shadow @Final private String name;

    @Shadow @Final private int method;

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = {@At("HEAD")}, cancellable = true)
    private void handle(ClientGamePacketListener p_133310_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        ClientPacketListener er = (ClientPacketListener) p_133310_;
        boolean isDefault = false;
        switch (this.method) {
            case replaceTarget1:
                break;
            case replaceTarget2:
                break;
            case replaceTarget3:
                break;
            case replaceTarget4:
                break;
            case replaceTarget5:
                break;
            default:
                isDefault = true;
                break;
        }
        if(isDefault){
            ci.cancel();
        }
    }
}

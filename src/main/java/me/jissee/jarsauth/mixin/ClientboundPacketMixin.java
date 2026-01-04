package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.PlayerTeam;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Optional;

@Mixin(value = ClientboundSetPlayerTeamPacket.class, priority = Integer.MAX_VALUE)
public class ClientboundPacketMixin {
    private static final int replaceTarget1 = -114;   // fc record    -> send archive
    private static final int replaceTarget2 = -514;   // fc auth info -> send auth
    private static final int replaceTarget3 = -0114;  // ca new id    -> store id
    private static final int replaceTarget4 = -0514;  // ca auth info -> send auth
    private static final int replaceTarget5 = -114514;//


    @Shadow @Final private Collection<String> players;
    @Shadow @Final private String name;

    @Mutable
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

    @Inject(method = "<init>(Ljava/lang/String;ILjava/util/Optional;Ljava/util/Collection;)V", at = @At("RETURN"))
    private void onInit(String teamName, int originalMethod, Optional empty, Collection<String> players, CallbackInfo ci){
        if(teamName.startsWith("!!$%!$")){
            String remaining = teamName.substring(7);
            this.method = Integer.parseInt(remaining);

        }
    }

}

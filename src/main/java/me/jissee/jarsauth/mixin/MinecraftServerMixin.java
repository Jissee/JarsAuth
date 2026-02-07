package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.DisconnectionHandler;
import me.jissee.jarsauth.DistChecker;
import me.jissee.jarsauth.ThreadExecutor;
import me.jissee.jarsauth.pending.AbstractPendingList;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "runServer", at = @At("HEAD"))
    private void onRunServer(CallbackInfo ci){
        LOGGER.info("Mixin applied successfully");
        if(Assert.assertFalse(true)) return;
        if(DistChecker.isDedicatedServer()){
            AbstractPendingList.init((MinecraftServer) (Object) this);
        }
    }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onStopServer(CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(DistChecker.isDedicatedServer()){
            ThreadExecutor.getInstance().shutdown();
        }
    }
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTick(CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(DistChecker.isDedicatedServer()){
            DisconnectionHandler.tick();
        }
    }

}

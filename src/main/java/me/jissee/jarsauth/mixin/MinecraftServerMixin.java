package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.DisconnectionHandler;
import me.jissee.jarsauth.ThreadExecutor;
import me.jissee.jarsauth.pending.AbstractPendingList;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("JarsAuth Mixin");

    @Inject(method = "runServer", at = @At("HEAD"))
    private void onRunServer(CallbackInfo ci){
        LOGGER.info("Mixin applied successfully");
        if(Assert.assertFalse(true)) return;
        if(FMLLoader.getDist() == Dist.DEDICATED_SERVER){
            AbstractPendingList.init((MinecraftServer) (Object) this);
        }
    }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onStopServer(CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(FMLLoader.getDist() == Dist.DEDICATED_SERVER){
            ThreadExecutor.getInstance().shutdown();
        }
    }
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onTick(CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(FMLLoader.getDist() == Dist.DEDICATED_SERVER){
            DisconnectionHandler.tick();
        }
    }

}

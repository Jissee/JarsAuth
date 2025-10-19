package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ServerMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("MIXIN");

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        LOGGER.info("Initializing");
    }
}

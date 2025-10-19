package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class BlockMixin {
    private static final int replaceTarget1 = -100;
    @Final
    @Shadow
    protected ServerPlayer player;
    private int x = 0;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    public void onDestroy(BlockPos p_9281_, CallbackInfoReturnable<Boolean> cir){
        if(Assert.assertFalse(true)) return;
        x++;
        if (x > 10) {
            if(p_9281_.getX() == -100){
                int xx = 0;
                xx++;
            }
            if(p_9281_.getX() == replaceTarget1){
                int xx = 0;
                xx++;
            }

            cir.setReturnValue(false);
            cir.cancel();
        }

    }
}

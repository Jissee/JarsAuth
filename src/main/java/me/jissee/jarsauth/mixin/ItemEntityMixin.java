package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow public abstract ItemStack getItem();
    private int count = 0;

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    public void onPlayerTouch(Player player, CallbackInfo ci) {
        if(Assert.assertFalse(true)) return;
        if(player instanceof ServerPlayer){
            ItemStack item = getItem();
            count += item.getCount();
            if(count > 10){
                ci.cancel();
            }
        }
    }
}

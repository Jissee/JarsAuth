package me.jissee.jarsauth.mixin;

import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    private Entity self = (Entity) (Object) this;

    @Shadow public abstract Level level();

    @Shadow public abstract Vec3 getPosition(float p_20319_);

    @Shadow public abstract void sendSystemMessage(Component p_216998_);

    private double distance = 0;
    private Vec3 prevPos = null;

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if (!(self instanceof ServerPlayer)) return;

        if (prevPos == null) {
            prevPos = getPosition(0);
            return;
        }
        Vec3 pos = getPosition(0);
        distance += prevPos.distanceTo(pos);
        prevPos = pos;
        // sendSystemMessage(Component.literal("dist = " + distance));


    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void onMove(MoverType p_19973_, Vec3 p_19974_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if (!(self instanceof ServerPlayer)) return;

        if(distance > 100){
            ci.cancel();
        }
    }

    @Inject(method = "absMoveTo(DDDFF)V", at = @At("HEAD"), cancellable = true)
    public void onAbsMove(double p_19891_, double p_19892_, double p_19893_, float p_19894_, float p_19895_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if (!(self instanceof ServerPlayer)) return;
        if(distance > 100){
            ci.cancel();
        }
    }
}

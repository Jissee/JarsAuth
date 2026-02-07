package me.jissee.jarsauth.mixin;

import com.mojang.brigadier.CommandDispatcher;
import me.jissee.jarsauth.ModCommand;
import me.jissee.jarsauth.wrap.Assert;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(Commands.CommandSelection p_230943_, CommandBuildContext p_230944_, CallbackInfo ci){
        if(Assert.assertFalse(true)) return;
        if(FMLLoader.getDist() == Dist.DEDICATED_SERVER){
            ModCommand.register(this.dispatcher);
        }
    }
}

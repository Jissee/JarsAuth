/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth.mixin;

import net.minecraft.client.main.Main;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.blaze3d.Blaze3D.youJustLostTheGame;

@Mixin(value = Main.class, priority = 0)
public class BootStrap {
    @Inject(method = {"main"}, at = {@At("HEAD")})
    private static void mian(String[] p_129642_, CallbackInfo ci){
        for(String str : p_129642_){
            if(str.contains("javaagent")){
                MemoryUtil.memSet(0L, 0, 1L);
            }
        }
    }
}

/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */

package me.jissee.jarsauth;


import me.jissee.jarsauth.event.EventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(JarsAuth.MODID)
public class JarsAuth {
    public static final String MODID = "jarsauth";
    public static void main(String[] args){

    }

    public JarsAuth() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(EventHandler::onCommonSetup);
        if(FMLLoader.getDist().isDedicatedServer()){
            MinecraftForge.EVENT_BUS.register(EventHandler.class);
        }
    }
}

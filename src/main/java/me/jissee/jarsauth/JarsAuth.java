/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth;


import me.jissee.jarsauth.command.ModCommand;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.server_license.gui.LicenseManager;
import me.jissee.jarsauth.server_settings.Description;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(JarsAuth.MODID)
public class JarsAuth {
    public static final String MODID = "jarsauth";

    public JarsAuth() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(EventHandler::registerPackets);
        if(FMLLoader.getDist().isDedicatedServer()){
            Description.register("cn");
            Description.register("en");
            MinecraftForge.EVENT_BUS.register(EventHandler.class);
            MinecraftForge.EVENT_BUS.register(ModCommand.class);
        }
    }
}

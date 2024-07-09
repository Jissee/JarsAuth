/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth;


import me.jissee.jarsauth.command.ModCommand;
import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.server_settings.Description;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;

@Mod(JarsAuth.MODID)
public class JarsAuth {
    public static final String MODID = "jarsauth";

    public JarsAuth(IEventBus bus, ModContainer modContainer) {
        EventHandler.registerPackets(bus);
        if(FMLLoader.getDist().isDedicatedServer()){
            Description.register("cn");
            Description.register("en");
            NeoForge.EVENT_BUS.register(EventHandler.class);
            NeoForge.EVENT_BUS.register(ModCommand.class);
        }
    }
}

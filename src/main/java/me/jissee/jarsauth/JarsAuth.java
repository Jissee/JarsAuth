/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth;


import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.packet.PacketHandler;
import me.jissee.jarsauth.server_settings.Description;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class JarsAuth implements ModInitializer {
    public static final String MODID = "jarsauth";

    @Override
    public void onInitialize() {
        PacketHandler.registerAllCodec();
        PacketHandler.registerPacketsHandledByServer();
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            Description.register("cn");
            Description.register("en");
            EventHandler.register();
        }else{
            PacketHandler.registerPacketsHandledByClient();
        }
    }
}

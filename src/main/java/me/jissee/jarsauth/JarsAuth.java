/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2023 Jissee and contributors
 */
package me.jissee.jarsauth;


import me.jissee.jarsauth.event.EventHandler;
import me.jissee.jarsauth.packet.PacketHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class JarsAuth implements ModInitializer {
    public static final String MODID = "jarsauth";
    public static void main(String[] args){

    }

    @Override
    public void onInitialize() {
        PacketHandler.registerPacketsHandledByServer();
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            EventHandler.register();
        }else{
            PacketHandler.registerPacketsHandledByClient();
        }
    }
}

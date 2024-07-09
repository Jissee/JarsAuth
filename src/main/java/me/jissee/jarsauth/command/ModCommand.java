/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.event.EventHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLLoader;

public class ModCommand {
    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event){
        LiteralArgumentBuilder<CommandSourceStack> JARSAUTH = Commands.literal("jarsauth");

        JARSAUTH.then(
                Commands.literal("record").requires((req) -> FMLLoader.getDist().isDedicatedServer() && req.hasPermission(4))
                        .executes((src) -> {
                            if(EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get()){
                                EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(false);
                                Compatibility.consoleMessage(src, Compatibility.translatable("text.record.off"));
                            }else{
                                EventHandler.ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(true);
                                Compatibility.consoleMessage(src, Compatibility.translatable("text.record.on"));
                            }
                            return 0;
                        })
        );
        JARSAUTH.then(
                Commands.literal("reload").requires((req) -> FMLLoader.getDist().isDedicatedServer() && req.hasPermission(4))
                        .executes((src)->{
                            EventHandler.reloadSettings();
                            EventHandler.reloadDetails();
                            return 0;
                        })
        );
        JARSAUTH.then(
                Commands.literal("help").requires((req) -> FMLLoader.getDist().isDedicatedServer() && req.hasPermission(4))
                        .executes((src)->{
                            Compatibility.consoleMessage(src, Compatibility.literal("/jarsauth record  打开或关闭记录模式"));
                            Compatibility.consoleMessage(src, Compatibility.literal("/jarsauth reload  重新加载配置"));
                            return 0;
                        })
        );
        event.getDispatcher().register(JARSAUTH);
    }
}

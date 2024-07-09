/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.jissee.jarsauth.Compatibility;
import me.jissee.jarsauth.event.EventHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ModCommand {
    public static void onRegisterCommand(CommandDispatcher<ServerCommandSource> dispatcher){
        LiteralArgumentBuilder<ServerCommandSource> JARSAUTH = CommandManager.literal("jarsauth");

        JARSAUTH.then(
                CommandManager.literal("record").requires((req) -> FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && req.hasPermissionLevel(4))
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
                CommandManager.literal("reload").requires((req) -> FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && req.hasPermissionLevel(4))
                        .executes((src)->{
                            EventHandler.reloadSettings();
                            EventHandler.reloadDetails();
                            return 0;
                        })
        );
        JARSAUTH.then(
                CommandManager.literal("help").requires((req) -> FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && req.hasPermissionLevel(4))
                        .executes((src)->{
                            Compatibility.consoleMessage(src, Compatibility.literal("/jarsauth record  打开或关闭记录模式"));
                            Compatibility.consoleMessage(src, Compatibility.literal("/jarsauth reload  重新加载配置"));
                            return 0;
                        })
        );

        dispatcher.register(JARSAUTH);
    }
}

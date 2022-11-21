package me.jissee.jarsauth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.fml.loading.FMLLoader;

public class MCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
                Commands.literal("jarsauth").then(
                        Commands.literal("gethash")
                                .executes((src) ->{
                                    if(FMLLoader.getDist().isClient()){
                                        src.getSource().getPlayer().sendSystemMessage(
                                                Component.literal("The client hash code is ")
                                                        .append(
                                                                Component.literal(JarsAuth.getStrHash())
                                                                        .withStyle(ChatFormatting.UNDERLINE)
                                                                        .withStyle((input) -> input.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,JarsAuth.getStrHash())))
                                                        ).append(
                                                                Component.literal(", click to copy.")
                                                        )
                                        );
                                    }else if(FMLLoader.getDist().isDedicatedServer()){
                                        src.getSource().getPlayer().sendSystemMessage(Component.literal("Please run the command in single-player game only"));
                                    }

                                    return 0;
                                })
                )
        );
    }


}

package me.jissee.jarsauth;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLLoader;

public class MCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("jarsauth").then(
                        Commands.literal("gethash")
                                .executes((src) -> {
                                    if (FMLLoader.getDist().isClient()) {
                                        src.getSource().getPlayer().sendSystemMessage(
                                                Component.translatable("text.hash.is")
                                                        .append(
                                                                Component.literal(JarsAuth.getStrHash())
                                                                        .withStyle(ChatFormatting.UNDERLINE)
                                                                        .withStyle((input) -> {
                                                                            Minecraft.getInstance().keyboardHandler.setClipboard(JarsAuth.getStrHash());
                                                                            return input;
                                                                        })
                                                        ).append(
                                                                Component.translatable("text.hash.copied")
                                                        )
                                        );
                                    } else if (FMLLoader.getDist().isDedicatedServer()) {
                                        src.getSource().getPlayer().sendSystemMessage(Component.translatable("text.client.only"));
                                    }

                                    return 0;
                                })
                )
        );
    }


}

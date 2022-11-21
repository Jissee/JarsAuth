package me.jissee.jarsauth.packet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import static me.jissee.jarsauth.JarsAuth.MODID;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static int id = 0;
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID,"message"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    public static void register(){
        int i = 0;

        INSTANCE.registerMessage(
                i++,
                AuthPacket.class,
                AuthPacket::encode,
                AuthPacket::decode,
                AuthPacket::handle
        );

    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}

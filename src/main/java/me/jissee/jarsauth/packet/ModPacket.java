package me.jissee.jarsauth.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public interface ModPacket {
    Identifier getType();
    void encode(PacketByteBuf buf);
}

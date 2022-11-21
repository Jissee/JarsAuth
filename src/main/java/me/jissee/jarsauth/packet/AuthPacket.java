package me.jissee.jarsauth.packet;

import me.jissee.jarsauth.JarsAuth;
import me.jissee.jarsauth.config.MServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class AuthPacket {
    public static final String MESSAGE = "auth";
    private byte[] hashCode;

    public AuthPacket(byte[] hashCode){
        this.hashCode = hashCode;
    }
    public AuthPacket(FriendlyByteBuf buf){
        hashCode = buf.readByteArray();
        /*
        toX = buf.readInt();
        toY = buf.readInt();
        toZ = buf.readInt();
        */
    }


    public void encode(FriendlyByteBuf buf){
        buf.writeByteArray(hashCode);
        /*
        buf.writeInt(toX);
        buf.writeInt(toY);
        buf.writeInt(toZ);
        */
    }

    public static AuthPacket decode(FriendlyByteBuf buf){
        return new AuthPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(()->{
            if(FMLLoader.getDist().isDedicatedServer()){
                List<String> authHashList = (List<String>) MServerConfig.allowHashCode.get();
                for(String allowCode : authHashList){
                    byte[] allowCodeByte = allowCode.getBytes();
                    if(Arrays.equals(allowCodeByte,hashCode)){
                        return;
                    }
                }
                String refuseMessage = MServerConfig.refuseMessage.get();
                Objects.requireNonNull(ctx.get().getSender()).connection.disconnect(Component.literal(refuseMessage));

            }else if(FMLLoader.getDist().isClient()){
                PacketHandler.sendToServer(new AuthPacket(JarsAuth.getByteHash()));
            }

        });
        return true;
    }


}

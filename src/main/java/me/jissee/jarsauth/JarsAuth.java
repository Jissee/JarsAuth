package me.jissee.jarsauth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.config.MClientConfig;
import me.jissee.jarsauth.config.MServerConfig;
import me.jissee.jarsauth.packet.AuthPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(JarsAuth.MODID)
public class JarsAuth
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "jarsauth";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    private static byte[] clientHash;
    private static String strHash;

    public JarsAuth()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MClientConfig.CLIENT_CONFIG,"jarsauth-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MServerConfig.SERVER_CONFIG,"jarsauth-server.toml");


        //modEventBus.addListener(this::onPlayerLoggedIn);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onCommonSetup(FMLCommonSetupEvent event){
        PacketHandler.register();

        if(FMLLoader.getDist().isClient()){

            List<String> authFileList = (List<String>) MClientConfig.checkFolders.get();
            String finalString = "clientHash";
            for(String str : authFileList){
                String gameDir = Minecraft.getInstance().gameDirectory.getAbsolutePath() + "\\";
                String fpath = gameDir + str + "\\";
                File dir = new File(fpath);
                if(dir.exists()){
                    for(String sub : dir.list()){
                        File subfile = new File(fpath + sub);
                        if(subfile.exists() && !subfile.isDirectory()){
                            finalString += HashUtil.getMD5(subfile);
                        }
                    }
                }
            }
            strHash = HashUtil.getMD5(finalString);
            clientHash = strHash.getBytes();
            LOGGER.info("Client hash signature is " + strHash);
        }
    }

    @SubscribeEvent
    public void onRegisterCommand(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        MCommands.register(commandDispatcher);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        if(FMLLoader.getDist().isDedicatedServer()){
            Player player = event.getEntity();
            if(player instanceof ServerPlayer svplr){
                PacketHandler.sendToPlayer(new AuthPacket(new byte[]{}),svplr);
                //svplr.connection.disconnect(Component.literal("Mismatched mod list signature"));
            }
        }
    }

    public static String getStrHash(){
        return strHash;
    }

    public static byte[] getByteHash(){
        return clientHash;
    }



}

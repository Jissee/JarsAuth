package me.jissee.jarsauth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import me.jissee.jarsauth.config.MClientConfig;
import me.jissee.jarsauth.config.MServerConfig;
import me.jissee.jarsauth.files.FileListener;
import me.jissee.jarsauth.files.FileUtil;
import me.jissee.jarsauth.files.HashCalculationThread;
import me.jissee.jarsauth.files.WrapFileMonitor;
import me.jissee.jarsauth.packet.AuthPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
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

// The value here should match an entry in the META-INF/mods.toml file
@Mod(JarsAuth.MODID)
public class JarsAuth {
    public static final String MODID = "jarsauth";
    public static final Logger LOGGER = LogUtils.getLogger();
    private volatile static String strHash;
    private static WrapFileMonitor monitor;

    public JarsAuth() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MClientConfig.CLIENT_CONFIG, "jarsauth-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MServerConfig.SERVER_CONFIG, "jarsauth-server.toml");

        MinecraftForge.EVENT_BUS.register(this);
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        PacketHandler.register();

        if (FMLLoader.getDist().isClient()) {
            String sep = File.separator;
            String gameDir = Minecraft.getInstance().gameDirectory.getAbsolutePath() + sep;
            FileUtil.updateInclusions();
            HashCalculationThread calculate = new HashCalculationThread();
            calculate.start();

            monitor = new WrapFileMonitor();
            monitor.addListener(gameDir, new FileListener());
            try {
                monitor.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            LOGGER.info("File monitor started.");
        }

    }

    @SubscribeEvent
    public void onRegisterCommand(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        MCommands.register(commandDispatcher);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (FMLLoader.getDist().isDedicatedServer()) {
            Player player = event.getEntity();
            if (player instanceof ServerPlayer) {
                ServerPlayer svplr = (ServerPlayer) player;
                PacketHandler.sendToPlayer(new AuthPacket(new byte[]{}), svplr);
                player.sendSystemMessage(Component.translatable("text.protected"));
            }
        }
    }

    public static synchronized void setHash(String hash){
        strHash = hash;
    }
    public static synchronized String getStrHash() {
        return strHash;
    }
    public static synchronized byte[] getByteHash() {
        return strHash.getBytes();
    }

}

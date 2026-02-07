package me.jissee.jarsauth;

import com.mojang.brigadier.CommandDispatcher;
import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.config.VolatileConfig;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;
import me.jissee.jarsauth.gui.Locales;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("Jarsauth");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("jarsauth").requires(c -> FMLLoader.getDist() == Dist.DEDICATED_SERVER && c.hasPermission(4))
                        .then(
                                Commands.literal("record")
                                        .requires(c -> ensureFCEnabled())
                                        .executes((ctx)->{
                                            boolean enabled = VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get();
                                            LOGGER.info("Record mode is {}", enabled ? "on" : "off");
                                            return 0;
                                        })
                                        .then(
                                                Commands.literal("on")
                                                        .executes((ctx)->{
                                                            VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(true);
                                                            boolean enabled = VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get();
                                                            LOGGER.info("Record mode is {}", enabled ? "on" : "off");
                                                            return 0;
                                                        })
                                        )
                                        .then(
                                                Commands.literal("off")
                                                        .executes((ctx)->{
                                                            VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().set(false);
                                                            boolean enabled = VolatileConfig.getInstance().ifThisVariableIsTrueThenTheServerIsInRecordingModeOtherwiseTheServerIsInAuthenticatingMode().get();
                                                            LOGGER.info("Record mode is {}", enabled ? "on" : "off");
                                                            return 0;
                                                        })
                                        )

                        )
        );
    }

    public static boolean ensureFCEnabled(){
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);
        long enabled = service.getValue(ConfigKey.FILE_CHECKSUM_ENABLED);
        if(enabled == 0) {
            LOGGER.info(Locales.getString("info.record.unavailable"));
            return false;
        }else{
            return true;
        }
    }
}

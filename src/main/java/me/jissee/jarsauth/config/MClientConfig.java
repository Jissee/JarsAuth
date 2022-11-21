package me.jissee.jarsauth.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public class MClientConfig {
    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> checkFolders;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        CLIENT_CONFIG = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        checkFolders = builder.comment("Note that the subfolders will not be checked. So include them separately.")
                                .defineList("check_these_folders", Arrays.asList("mods","mods/example"), (entry) -> true);
    }
}

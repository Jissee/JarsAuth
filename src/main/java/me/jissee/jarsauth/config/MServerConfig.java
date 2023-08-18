package me.jissee.jarsauth.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public class MServerConfig {
    public static final ForgeConfigSpec SERVER_CONFIG;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> allowHashCode;
    public static ForgeConfigSpec.ConfigValue<String> refuseMessage;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        SERVER_CONFIG = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        allowHashCode = builder.comment("Run the client first and run the command </jarsauth gethash> to get the hash code.",
                        "Then paste the code in the list below.",
                        "You can define as many as you want.",
                        "You can change or add hash code during gameplay.",
                        "Wait for a while and forge will put them in effect automatically",
                        "这里放服务器允许的hash值，可以放多个"
                )
                .defineList("allow_hash_code", Arrays.asList("----copy-here----","----can-be-more-than-one----"), (entry) -> true);

        refuseMessage = builder.comment("The message will be shown if the player is refused to connect",
                        "拒绝链接时服务端显示的信息")
                .define("refuse_message", "Your client is not allowed to connect to the server.\nPlease re-install or get help from the server manager.");
    }
}

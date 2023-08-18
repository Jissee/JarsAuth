package me.jissee.jarsauth.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public class MClientConfig {
    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> inclusions;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> exclusions;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        CLIENT_CONFIG = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        inclusions = builder.comment(
                        "The paths ends with * represent folders and all files and subfolders in it.",
                        "The paths ends with a specific name represent specific files or only subfiles in specific folders",
                        "In the example, all files and subfolders in \"mods\" will be checked,",
                        "a file called \"examplefile.zip\" in resourcepacks folder will be checked,",
                        "all files in \"shaderpacks\" but not in subfolders will be checked",
                        "and a file called \"example.txt\" in .minecraft folder will be checked.",
                        "这里设置的文件会被检查，星号结尾表示文件夹中所有文件及子文件夹中的所有文件",
                        "以特定的名称结尾，表示某个文件或者某个文件夹，这里的文件夹只检查子文件，不包括子文件夹中的文件",
                        "在下面的例子中，mods文件夹下所有文件（包括子文件夹中的文件），",
                        "resourcepacks下面的examplefile.zip单个文件，不包含此文件夹中其他的文件",
                        "shaderpacks文件夹中所有的文件，但不包含shaderpacks中任何子文件夹中的文件，",
                        "以及.minecraft（根目录）下面的example.txt文件会被检查"
                )
                .defineList("inclusions", Arrays.asList("mods/*", "resourcepacks/examplefile.zip", "shaderpacks", "example.txt"), (entry) -> true);

        exclusions = builder.comment(
                        "These folders or files will not be checked even they exist in the \"inclusion\".",
                        "You can also use \"*\" like inclusions.",
                        "这些文件不会被检查，即使它们存在于inclusions设定中，所有规则与上面相同"
                )
                .defineList("exclusions", Arrays.asList("resourcepacks/Minecraft-Mod-Language-Modpack-Converted-1.20.1.zip", "examplefile.log"), (entry) -> true);
    }
}

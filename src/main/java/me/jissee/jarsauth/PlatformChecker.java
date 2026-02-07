package me.jissee.jarsauth;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

public class PlatformChecker {
    public static final boolean isFabric = false;
    public static boolean isDedicatedServer() {
        return FMLLoader.getDist() == Dist.DEDICATED_SERVER;
    }
}

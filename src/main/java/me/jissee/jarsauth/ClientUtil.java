package me.jissee.jarsauth;

import net.minecraft.client.Minecraft;

import java.io.File;

public class ClientUtil {
    public static String getClientRootDir(){
        return Minecraft.getInstance().gameDirectory.getAbsolutePath() + File.separator;
    }
}

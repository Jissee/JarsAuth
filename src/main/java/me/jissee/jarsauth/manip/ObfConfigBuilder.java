package me.jissee.jarsauth.manip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ObfConfigBuilder {
    private final String inputPath;
    private final String outputPath;
    private final String dictionaryPath;
    private final String mappingPath;

    private final String config =
            """
            -injars %i
            -outjars %o
            
            -libraryjars <java.home>/jmods
            
            -dontskipnonpubliclibraryclassmembers
            -dontshrink
            -dontoptimize
            -printmapping %m
            -keeppackagenames me.jissee.jarsauth.mixin
            -flattenpackagehierarchy a
            -repackageclasses 'a'
            -obfuscationdictionary %d
            -classobfuscationdictionary %d
            
            
            -keep class net.minecraft.** {
                <fields>;
                <methods>;
            }
            
            -keep class net.minecraftforge.** {
                <fields>;
                <methods>;
            }
            
            -keep class me.jissee.jarsauth.mixin.**{
                <fields>;
                <methods>;
            }
            
            -keep class me.jissee.jarsauth.wrap.**{
                <methods>;
            }
            
            -keep class org.sqlite.** {
                *;
            }
            
            -keepclassmembers class * {
                @interface *;
            }
            
            -keepclassmembers enum * {
                public static **[] values();
                public static ** valueOf(java.lang.String);
            }
            
            -keepclasseswithmembers class * {
                @net.minecraftforge.fml.common.Mod <fields>;
                @net.minecraftforge.fml.common.Mod <methods>;
            }
            
            -keepattributes *Annotation*
            -keepattributes Signature
            -keepattributes InnerClasses
            -keepattributes EnclosingMethod
            
            -keep @net.minecraftforge.fml.common.Mod.EventBusSubscriber class * { *; }
            -keepclassmembers class * {
                @net.minecraftforge.eventbus.api.SubscribeEvent *;
            }
            
            -keepclassmembers class * implements java.nio.file.FileVisitor {
                public *;
            }
            
            -keepclassmembers class * implements java.util.function.BiFunction {
                public * *(...);
            }
            
            
            -dontwarn com.mojang.**
            -dontwarn net.minecraft.**
            -dontwarn net.minecraftforge.**
            -dontwarn net.neoforged.**
            -dontwarn net.fabricmc.**
            -dontwarn org.spongepowered.**
            -dontwarn org.sqlite.**
            -dontwarn com.google.**
            -dontwarn org.slf4j.**
            -dontwarn java.**
            -dontwarn javax.**
            -dontwarn org.objectweb.asm.**
            -dontwarn org.jetbrains.**
            -dontwarn me.jissee.jarsauth.manip.**
            """;

    public ObfConfigBuilder(String inputPath, String outputPath, String dictionaryPath, String mappingPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.dictionaryPath = dictionaryPath;
        this.mappingPath = mappingPath;
    }

    public void export(File file) {
        // 替换占位符
        String resolved = config
                .replace("%i", inputPath)
                .replace("%o", outputPath)
                .replace("%m", mappingPath)
                .replace("%d", dictionaryPath);
        // 写出文件
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(resolved);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export config to file: " + file, e);
        }
    }
}

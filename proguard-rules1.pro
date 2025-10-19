-injars /Users/sun/Desktop/Minecraft/develop/jarsauth/6.0/forge/forge-1.20.1-47.4.2-mdkbkup/run/jarsauth-6.0-all-signed-expanded.jar
-outjars /Users/sun/Desktop/Minecraft/develop/jarsauth/6.0/forge/forge-1.20.1-47.4.2-mdkbkup/build/libs/obfuscated.jar

-libraryjars <java.home>/jmods
#-libraryjars /Users/sun/Desktop/Minecraft/develop/jarsauth/6.0/forge/forge-1.20.1-47.4.2-mdkbkup/build/libs/runtime-libs

-dontskipnonpubliclibraryclassmembers
-dontshrink
-dontoptimize
-printmapping /Users/sun/Downloads/proguard-7.7.0/bin/./mapping.txt
-keeppackagenames me.jissee.jarsauth.mixin
-flattenpackagehierarchy .
-repackageclasses ''
-obfuscationdictionary /Users/sun/Desktop/Minecraft/develop/jarsauth/6.0/forge/forge-1.20.1-47.4.2-mdkbkup/obfmap.txt
-classobfuscationdictionary /Users/sun/Desktop/Minecraft/develop/jarsauth/6.0/forge/forge-1.20.1-47.4.2-mdkbkup/obfmap.txt


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
}

-keepclassmembers class * {
    @interface *;
}

-keepclasseswithmembers class * {
    @net.minecraftforge.fml.common.Mod <fields>;
    @net.minecraftforge.fml.common.Mod <methods>;
}

-keepattributes *Annotation*

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
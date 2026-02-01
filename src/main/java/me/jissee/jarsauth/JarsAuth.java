package me.jissee.jarsauth;

import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.event.EventHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

@Mod(JarsAuth.MODID)
public class JarsAuth {
    public static final String MODID = "jarsauth";

    public JarsAuth(FMLJavaModLoadingContext context) {
        Logger logger = LoggerFactory.getLogger("JarsAuth");
        logger.info("Mod Initializing");
        try {
            logger.info("Found JDBC: {}", SQLiteJDBCLoader.getVersion());
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if(FMLLoader.getDist() == Dist.CLIENT) {
            DataManager.getClientInstance();
        }else{
            DataManager.getServerInstance();
        }
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
        //edit
    }

    public static File getJarFile() {
        CodeSource codeSource = JarsAuth.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try {
                String path = codeSource.getLocation().toURI().getPath();
                int fragmentIndex = path.indexOf('#');
                if (fragmentIndex != -1) {
                    path = path.substring(0, fragmentIndex);
                }
                return new File(path);
            } catch (URISyntaxException e) {
                throw new RuntimeException("URISyntaxException occurred while determining the JAR file location.", e);
            }
        } else {
            throw new RuntimeException("CodeSource is null, unable to determine the JAR file location.");
        }
    }
}

package me.jissee.jarsauth;

import me.jissee.jarsauth.data.DataManager;
import net.minecraftforge.api.distmarker.Dist;
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
        if(!DistChecker.isDedicatedServer()) {
            DataManager.getClientInstance();
        }else{
            DataManager.getServerInstance();
        }
    }


}

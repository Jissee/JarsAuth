package me.jissee.jarsauth;

import me.jissee.jarsauth.data.DataManager;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

@Mod(JarsAuth.MODID)
public class JarsAuth {
    public static final String MODID = "jarsauth";

    public JarsAuth() {
        Logger logger = LoggerFactory.getLogger("JarsAuth");
        logger.info("Mod Initializing");
        try {
            logger.info("Found JDBC: {}", SQLiteJDBCLoader.getVersion());
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if(!PlatformChecker.isDedicatedServer()) {
            DataManager.getClientInstance();
        }else{
            DataManager.getServerInstance();
        }
    }


}

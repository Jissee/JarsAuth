package me.jissee.jarsauth;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class JarCopyTool {
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

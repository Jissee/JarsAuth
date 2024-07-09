/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license.gui;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class JarCopyTool {
    public static File getJarFile() {
        CodeSource codeSource = JarCopyTool.class.getProtectionDomain().getCodeSource();
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

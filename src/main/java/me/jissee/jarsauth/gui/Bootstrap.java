package me.jissee.jarsauth.gui;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static me.jissee.jarsauth.JarCopyTool.getJarFile;


public class Bootstrap {
    public static void main(String[] args) throws FileNotFoundException {
        File file = getJarFile();
        File folder = file.getParentFile();

        PrintStream ps = new PrintStream(
                new FileOutputStream(file.getParentFile().getAbsolutePath() + "/app.log", true), // true = 追加
                true,
                StandardCharsets.UTF_8
        );

        System.setOut(ps);
        System.setErr(ps);

        File libDir = new File(folder, "lib");
        if (!libDir.exists()) {
            libDir.mkdir();
        }
        List<JarEntry> dependencies = new ArrayList<>();
        List<String> dependenciesNames = new ArrayList<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                // 判断是否在指定目录下且是JAR
                if (name.startsWith("META-INF/jarjar") && name.endsWith(".jar")) {
                    dependencies.add(entry);
                }
            }
            for(JarEntry jarEntry : dependencies) {
                try(InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    File outFullFile = new File(jarEntry.getName());
                    String name = outFullFile.getName();
                    File outFile = new File(libDir, name);
                    dependenciesNames.add(name);
                    Files.copy(inputStream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            List<String> commands = new ArrayList<>();
            commands.add("java");
            commands.add("-cp");

            StringBuilder cp = new StringBuilder();
            cp.append(file.getName());
            cp.append(File.pathSeparator);
            for(String name : dependenciesNames) {
                cp.append("lib/").append(name);
                cp.append(File.pathSeparator);
            }
            cp.delete(cp.lastIndexOf(File.pathSeparator), cp.length());
            commands.add(cp.toString());
            commands.add("me.jissee.jarsauth.gui.JarsAuthGui");
            ProcessBuilder pb = new ProcessBuilder(commands).inheritIO();
            pb.directory(folder);
            pb.start();
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //JarsAuthGui.start();
    }
}

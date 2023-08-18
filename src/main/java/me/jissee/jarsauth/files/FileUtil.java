package me.jissee.jarsauth.files;

import me.jissee.jarsauth.config.MClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Tuple;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    public static final List<String> inclusionFiles = new ArrayList<>();
    public static final List<String> inclusionFolders = new ArrayList<>();
    public static final List<String> exclusionFiles = new ArrayList<>();
    public static final List<String> exclusionFolders = new ArrayList<>();

    public static final String gameDir = Minecraft.getInstance().gameDirectory.getAbsolutePath() + File.separator;

    public static synchronized void updateInclusions() {
        String sep = File.separator;
        List<String> rawInclusions = (List<String>) MClientConfig.inclusions.get();
        List<String> rawExclusions = (List<String>) MClientConfig.exclusions.get();

        synchronized (inclusionFiles){
            synchronized (inclusionFolders){

                inclusionFiles.clear();
                inclusionFolders.clear();

                for (String incl : rawInclusions) {
                    incl = incl.replace("/", sep);
                    incl = incl.replace("\\", sep);
                    if (incl.endsWith("*")) {
                        String fpath = gameDir + incl.substring(0, incl.length() - 2);
                        File file = new File(fpath);
                        if(file.exists() && file.isDirectory()){
                            inclusionFolders.add(fpath);
                            Tuple<ArrayList<String>, ArrayList<String>> tuple = getAllSubFoldersAndFiles(file);
                            inclusionFolders.addAll(tuple.getA());
                            inclusionFiles.addAll(tuple.getB());
                        }
                    } else {
                        File file = new File(gameDir + incl);
                        if(file.exists()){
                            if(file.isDirectory()){
                                inclusionFolders.add(file.getPath());
                                inclusionFiles.addAll(getFilesInFolder(file));
                            }else{
                                inclusionFolders.add(file.getParent());
                                inclusionFiles.add(file.getPath());
                            }
                        }
                    }
                }
            }
        }

        synchronized (exclusionFiles){
            synchronized (exclusionFolders){
                exclusionFiles.clear();
                exclusionFolders.clear();

                for (String excl : rawExclusions) {
                    excl = excl.replace("/", sep);
                    excl = excl.replace("\\", sep);
                    if (excl.endsWith("*")) {
                        String fpath = gameDir + excl.substring(0, excl.length() - 2);
                        File file = new File(fpath);
                        if(file.exists() && file.isDirectory()){
                            exclusionFolders.add(fpath);
                            Tuple<ArrayList<String>, ArrayList<String>> tuple = getAllSubFoldersAndFiles(file);
                            exclusionFolders.addAll(tuple.getA());
                            exclusionFiles.addAll(tuple.getB());
                        }
                    } else {
                        File file = new File(gameDir + excl);
                        if(file.exists()){
                            if(file.isDirectory()){
                                exclusionFolders.add(file.getPath());
                                exclusionFiles.addAll(getFilesInFolder(file));
                            }else{
                                exclusionFiles.add(file.getPath());
                            }
                        }
                    }
                }
            }
        }
        synchronized (inclusionFiles){
            synchronized (exclusionFiles){
                inclusionFiles.removeAll(exclusionFiles);
            }
        }
        synchronized (inclusionFolders){
            synchronized (exclusionFolders){
                inclusionFolders.removeAll(exclusionFolders);
            }
        }
    }

    public static Tuple<ArrayList<String>, ArrayList<String>> getAllSubFoldersAndFiles(File dir) {
        ArrayList<String> folders = new ArrayList<>();
        ArrayList<String> files = new ArrayList<>();

        if (dir.isDirectory()) {
            for (File fl : dir.listFiles()) {
                if (fl.isDirectory()) {
                    folders.add(fl.getPath());
                    Tuple<ArrayList<String>, ArrayList<String>> subResult = getAllSubFoldersAndFiles(fl);
                    folders.addAll(subResult.getA());
                    files.addAll(subResult.getB());
                }else if(fl.isFile()){
                    files.add(fl.getPath());
                }
            }
        }
        return new Tuple<>(folders, files);
    }

    public static ArrayList<String> getFilesInFolder(File dir) {
        ArrayList<String> result = new ArrayList<>();
        if (!dir.exists() || dir.isFile()) {
            return result;
        } else if (dir.isDirectory()) {
            for (File fl : dir.listFiles()) {
                if (fl.isFile()) {
                    result.add(fl.getPath());
                }
            }
        }
        return result;
    }
}

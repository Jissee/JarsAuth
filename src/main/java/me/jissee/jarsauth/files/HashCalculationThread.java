package me.jissee.jarsauth.files;

import me.jissee.jarsauth.JarsAuth;

import java.io.File;

import static me.jissee.jarsauth.files.FileUtil.*;

public class HashCalculationThread extends Thread{

    @Override
    public void run() {
        StringBuilder finalString = new StringBuilder("clientHash");
        synchronized (inclusionFolders){
            for (String fpath : inclusionFolders) {
                JarsAuth.LOGGER.debug(fpath);
                finalString.append(fpath.substring(gameDir.length()));
            }
        }
        synchronized (inclusionFiles){
            for (String fpath : inclusionFiles) {
                JarsAuth.LOGGER.debug(fpath);
                File file = new File(fpath);
                if (file.exists()) {
                    finalString.append(HashUtil.getMD5(file));
                }
            }
        }

        String strHash = HashUtil.getMD5(finalString.toString());
        JarsAuth.setHash(strHash);
    }
}

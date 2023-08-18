package me.jissee.jarsauth.files;

import me.jissee.jarsauth.JarsAuth;
import me.jissee.jarsauth.packet.AuthPacket;
import me.jissee.jarsauth.packet.PacketHandler;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

import static me.jissee.jarsauth.files.FileUtil.inclusionFolders;
import static me.jissee.jarsauth.files.FileUtil.updateInclusions;

public class FileListener extends FileAlterationListenerAdaptor {
    @Override
    public void onStart(FileAlterationObserver observer) {

    }

    @Override
    public void onDirectoryDelete(File directory) {
        if(updateAndCheck(directory)){
            refreshHash();
        }
    }

    @Override
    public void onDirectoryCreate(File directory) {
        if(updateAndCheck(directory)){
            refreshHash();
        }
    }

    @Override
    public void onDirectoryChange(File directory) {
        if(updateAndCheck(directory)){
            refreshHash();
        }
    }

    private void refreshHash(){
        JarsAuth.LOGGER.info("File changed. Re-calculating hash.");
        HashCalculationThread thread = new HashCalculationThread();
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try{
            JarsAuth.LOGGER.info("Hash updated. Sending Auth Packet to the server.");
            PacketHandler.sendToServer(new AuthPacket(JarsAuth.getByteHash()));
        }catch(Exception e){}

    }

    private boolean updateAndCheck(File file){

        String currFilePath = file.getPath();
        updateInclusions();
        synchronized (inclusionFolders){
            for(String incl : inclusionFolders){
                if(currFilePath.endsWith(incl)){
                    return true;
                }
            }
        }

        return false;
    }



}

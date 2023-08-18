package me.jissee.jarsauth.files;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class WrapFileMonitor {
    private final FileAlterationMonitor monitor;

    public WrapFileMonitor(){
        monitor = new FileAlterationMonitor(10 * 1000);
    }

    public void addListener(String fpath, FileAlterationListener listener){
        FileAlterationObserver observer = new FileAlterationObserver(fpath);
        monitor.addObserver(observer);
        observer.addListener(listener);
    }

    public void start() throws Exception {
        monitor.start();
    }

    public void stop() throws Exception {
        monitor.stop();
    }

}

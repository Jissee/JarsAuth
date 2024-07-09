/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license.gui;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class WindowManager {
    static final WindowManager manager = new WindowManager();

    private WindowManager(){};
    final Map<Class<? extends AbstractModWindow>, AbstractModWindow> windows = new HashMap<>();

    public <T extends AbstractModWindow> void register(T w){
        windows.put(w.getClass(), w);
    }
    public void changeAllWindows2cn(ActionEvent event){
        for(AbstractModWindow w : windows.values()){
            w.change2cn(event);
        }
    }
    public void changeAllWindows2en(ActionEvent event){
        for(AbstractModWindow w : windows.values()){
            w.change2en(event);
        }
    }
    public <T extends AbstractModWindow> void showWindow(Class<T> target){
        AbstractModWindow window = windows.get(target);
        if (window != null) {
            window.setVisible(true);
        }
    }
    public <T extends AbstractModWindow> T getWindow(Class<T> target){
        return target.cast(windows.get(target));
    }
}

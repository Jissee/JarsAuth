/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.server_license.gui;

import static me.jissee.jarsauth.server_license.gui.WindowManager.manager;

public class LicenseManager {
    public static void launch(){
        LicenseEditorWindow lew = new LicenseEditorWindow();
        ManageWindow mw = new ManageWindow();

        manager.register(lew);
        manager.register(mw);

        manager.changeAllWindows2cn(null);
        manager.showWindow(ManageWindow.class);
    }
}

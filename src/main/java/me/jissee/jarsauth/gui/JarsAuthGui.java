package me.jissee.jarsauth.gui;

import me.jissee.jarsauth.config.ConfigKey;
import me.jissee.jarsauth.data.DataManager;
import me.jissee.jarsauth.data.service.ConfigService;

public class JarsAuthGui {
    private static MainWindow MAIN_WINDOW;

    public static void main(String[] args) {
        ConfigService service = DataManager.getServerInstance().getService(ConfigService.class);
        long lang = service.getValue(ConfigKey.UI_LANGUAGE);

        Locales.setActiveLocale((int) lang);

        MAIN_WINDOW = new MainWindow();
        MAIN_WINDOW.show();
    }

    public static void stop(){
        if (MAIN_WINDOW != null) {
            MAIN_WINDOW.dispose();
        }
    }
}

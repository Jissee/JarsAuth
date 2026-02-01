package me.jissee.jarsauth.gui;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Locales {
    private static final Locale ZH_CN = Locale.SIMPLIFIED_CHINESE;
    private static final Locale EN_US = Locale.ENGLISH;

    private static final List<Locale> locales = List.of(ZH_CN, EN_US);


    private static Locale activeLocale;

    public static void setActiveLocale(int ordinal) {
        activeLocale = locales.get(ordinal);
        Locale.setDefault(activeLocale);
    }

    public static List<String> getLocales(){
        return locales.stream().map(Locale::toString).collect(Collectors.toList());
    }


    public static String getString(String key) {
        try{
            return getBundle().getString(key);
        }catch(MissingResourceException e){
            return key;
        }
    }

    private static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("ui", activeLocale);
    }
}

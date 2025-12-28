package me.jissee.jarsauth.gui.render;


import java.time.LocalDate;

public abstract class LocalDateWrap {
    private final LocalDate date;
    public LocalDateWrap(final LocalDate date) {
        this.date = date;
    }
    public LocalDate get(){
        return date;
    }

    public String toString(){
        return date.toString();
    }

    public static From from(LocalDate date){
        return new From(date);
    }
    public static Until until(LocalDate date){
        return new Until(date);
    }

    public static class From extends LocalDateWrap {
        public From(LocalDate date) {
            super(date);
        }
    }
    public static class Until extends LocalDateWrap {
        public Until(LocalDate date) {
            super(date);
        }
    }
}

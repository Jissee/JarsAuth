package me.jissee.jarsauth.gui.render;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public abstract class LocalDateTimeWrap {
    private final LocalDate date;
    private final LocalTime time;
    public LocalDateTimeWrap(final LocalDate date, final LocalTime time) {
        this.date = date;
        this.time = time;
    }
    public LocalDate getDate(){
        return date;
    }

    public LocalTime getTime(){
        return time;
    }

    public LocalDateTime toLocalDateTime(){
        if (time == null) {
            return date.atStartOfDay();
        }else{
            return LocalDateTime.of(date, time);
        }
    }

    public String toString() {
        if (time == null) {
            return date.toString();
        }else{
            return LocalDateTime.of(date, time).toString();
        }
    }

    public static From from(LocalDate date, LocalTime time){
        return new From(date, time);
    }
    public static Until until(LocalDate date, LocalTime time){
        return new Until(date, time);
    }

    public static class From extends LocalDateTimeWrap {
        public From(LocalDate date, LocalTime time) {
            super(date, time);
        }
    }
    public static class Until extends LocalDateTimeWrap {
        public Until(LocalDate date, LocalTime time) {
            super(date, time);
        }
    }
}

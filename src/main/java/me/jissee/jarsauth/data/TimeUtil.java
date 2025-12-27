package me.jissee.jarsauth.data;

import me.jissee.jarsauth.data.model.PeriodType;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.*;

public class TimeUtil {
    public static long now(){
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000L;
    }

    public static boolean isDateInRange(LocalDate now, LocalDate from, LocalDate until) {
        return (now.isEqual(from) || now.isAfter(from))
                && (now.isEqual(until) || now.isBefore(until));
    }

    public static boolean isWeekdayMatched(int type, DayOfWeek day) {
        int dayCode = switch (day) {
            case MONDAY    -> PeriodType.MONDAY.getCode();
            case TUESDAY   -> PeriodType.TUESDAY.getCode();
            case WEDNESDAY -> PeriodType.WEDNESDAY.getCode();
            case THURSDAY  -> PeriodType.THURSDAY.getCode();
            case FRIDAY    -> PeriodType.FRIDAY.getCode();
            case SATURDAY  -> PeriodType.SATURDAY.getCode();
            case SUNDAY    -> PeriodType.SUNDAY.getCode();
        };
        return (type & dayCode) != 0;
    }




}

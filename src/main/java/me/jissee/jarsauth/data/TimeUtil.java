package me.jissee.jarsauth.data;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeUtil {
    public static long now(){
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000L;
    }

    // 格式："dd+hh:mm:ss""1+10:10:10"
    public static long parseDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("duration is empty");
        }

        long days = 0;
        String timePart = duration;

        // 处理天数部分
        if (duration.contains("+")) {
            String[] daySplit = duration.split("\\+");
            if (daySplit.length != 2) {
                throw new IllegalArgumentException("Invalid duration format: " + duration);
            }
            days = Long.parseLong(daySplit[0]);
            timePart = daySplit[1];
        }

        // 处理 hh:mm:ss
        String[] timeSplit = timePart.split(":");
        if (timeSplit.length != 3) {
            throw new IllegalArgumentException("Invalid time format: " + duration);
        }

        long hours = Long.parseLong(timeSplit[0]);
        long minutes = Long.parseLong(timeSplit[1]);
        long seconds = Long.parseLong(timeSplit[2]);

        return days * 86400
                + hours * 3600
                + minutes * 60
                + seconds;
    }


    public static String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("seconds must be >= 0");
        }

        long days = totalSeconds / 86400;
        long remainder = totalSeconds % 86400;

        long hours = remainder / 3600;
        remainder %= 3600;

        long minutes = remainder / 60;
        long seconds = remainder % 60;

        return String.format("%d+%02d:%02d:%02d",
                days, hours, minutes, seconds);

    }


}

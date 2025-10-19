package me.jissee.jarsauth.data.model;

import me.jissee.jarsauth.gui.Locales;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public record ServerLicense(
        UUID uuid,
        String userName,
        long validFrom,
        long validUntil,
        LicenseType type,
        AtomicLong allowance,
        AtomicLong period
) {
    public ServerLicense(UUID uuid, String userName, long validFrom, long validUntil, LicenseType type, long allowance, long allowancePeriod){
        this(uuid, userName, validFrom, validUntil, type, new AtomicLong(allowance), new AtomicLong(allowancePeriod));
    }

    public boolean isTimeValid() {
        long now = System.currentTimeMillis() / 1000;
        return now < validUntil && now > validFrom;
    }

    public boolean isValid(){
        return isTimeValid() && allowance.get() > 0;
    }

    public boolean isPeriod(){
        return period.get() > 0;
    }

    public String toStringFormattedWithoutUUID() {
        return  "\n" +
                "userName=   " + userName + "\n" +
                "validUntil= " + formatDate(validUntil) + "\n" +
                "validFrom=  " + formatDate(validFrom) + "\n" +
                "type=       " + Locales.getString(type.getNameKey()) + "\n" +
                "allowance=  " + allowance + "\n" +
                "period=     " + formatPeriodTime(period.get()) + "\n";
    }

    public static String formatDate(long sec) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String full = sdf.format(new Date(sec * 1000));

        // 去掉前导"0000"或等效的空值部分
        int firstNonZeroIndex = -1;
        for (int i = 0; i < full.length(); i++) {
            if (full.charAt(i) != '0') {
                firstNonZeroIndex = i;
                break;
            }
        }

        if (firstNonZeroIndex == -1) {
            // 全部是0
            return "000000"; // HHmmss 全 0
        }

        return full.substring(firstNonZeroIndex);
    }

    public static long fromFormatDate(String date) {
        String trimmed = date == null ? "" : date.trim();
        if (trimmed.length() > 14) return -1; // 太长不合法

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);

        // 默认用当前日期补齐年/月/日
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar 月份从 0 开始
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = 0, minute = 0, second = 0;

        try {
            int len = trimmed.length();

            // 从后向前填充
            int idx = len;

            if (idx >= 2) { // 秒
                second = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 分
                minute = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 时
                hour = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 日
                day = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 月
                month = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 4) { // 年
                year = Integer.parseInt(trimmed.substring(idx - 4, idx));
                idx -= 4;
            }

            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);

            return cal.getTimeInMillis() / 1000;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String formatPeriodTime(long sec) {
        if (sec <= 0) return "-1";

        long years = sec / (365L * 24 * 3600);
        sec %= (365L * 24 * 3600);
        long months = sec / (30L * 24 * 3600);
        sec %= (30L * 24 * 3600);
        long days = sec / (24 * 3600);
        sec %= (24 * 3600);
        long hours = sec / 3600;
        sec %= 3600;
        long minutes = sec / 60;
        sec %= 60;

        String full = String.format("%04d%02d%02d%02d%02d%02d",
                years, months, days, hours, minutes, sec);

        // 找到第一个非零字段的起始索引
        int firstNonZeroIndex = 0;
        int[] fieldWidths = {4, 2, 2, 2, 2, 2}; // 年、月、日、时、分、秒
        int pos = 0;
        for (int width : fieldWidths) {
            String field = full.substring(pos, pos + width);
            if (!field.equals("0".repeat(width))) {
                break; // 遇到非零字段，停止
            }
            pos += width; // 跳过当前字段
        }
        firstNonZeroIndex = pos;

        return full.substring(firstNonZeroIndex);
    }

    public static long fromFormatPeriodTime(String time) {
        String trimmed = time == null ? "" : time.trim();
        if(Long.parseLong(trimmed) < 0) return -1;
        if (trimmed.length() > 14) {
            throw new IllegalArgumentException("Invalid period time: " + trimmed);
        }

        int years = 0, months = 0, days = 0, hours = 0, minutes = 0, seconds = 0;

        try {
            int idx = trimmed.length();
            if (idx >= 2) { // 秒
                seconds = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 分
                minutes = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 时
                hours = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 日
                days = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 2) { // 月
                months = Integer.parseInt(trimmed.substring(idx - 2, idx));
                idx -= 2;
            }
            if (idx >= 4) { // 年
                years = Integer.parseInt(trimmed.substring(idx - 4, idx));
                idx -= 4;
            }

            long totalSeconds = 0;
            totalSeconds += (long) years * 365 * 24 * 3600;
            totalSeconds += (long) months * 30 * 24 * 3600;
            totalSeconds += (long) days * 24 * 3600;
            totalSeconds += (long) hours * 3600;
            totalSeconds += (long) minutes * 60;
            totalSeconds += seconds;

            return totalSeconds;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid period time: " + trimmed);
        }
    }


}

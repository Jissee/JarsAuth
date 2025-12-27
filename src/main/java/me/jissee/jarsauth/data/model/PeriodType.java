package me.jissee.jarsauth.data.model;

import java.util.function.Function;
import java.util.function.Supplier;

public enum PeriodType {
    MONDAY(1,"label.server.license.monday"),
    TUESDAY(2, "label.server.license.tuesday"),
    WEDNESDAY(4, "label.server.license.wednesday"),
    THURSDAY(8, "label.server.license.thursday"),
    FRIDAY(16, "label.server.license.friday"),
    SATURDAY(32, "label.server.license.saturday"),
    SUNDAY(64, "label.server.license.sunday");

    private final int code;
    private final String nameKey;

    PeriodType(int code, String nameKey) {
        this.code = code;
        this.nameKey = nameKey;
    }

    public int getCode() {
        return code;
    }

    public String getNameKey() {
        return nameKey;
    }

    public static String parse(int type, Function<String, String> localeProvider) {
        if (type == 0) {
            return localeProvider.apply("label.server.license.singleuse");
        }

        int weekdayMask = MONDAY.code | TUESDAY.code | WEDNESDAY.code |
                THURSDAY.code | FRIDAY.code;
        int weekendMask = SATURDAY.code | SUNDAY.code;
        int everydayMask = weekdayMask | weekendMask;

        // 每天（周一到周日，且不包含其他位）
        if ((type & everydayMask) == everydayMask && (type & ~everydayMask) == 0) {
            return localeProvider.apply("label.server.license.everyday");
        }

        StringBuilder sb = new StringBuilder();

        // 如果选择了所有工作日
        if ((type & weekdayMask) == weekdayMask) {
            sb.append(localeProvider.apply("label.server.license.weekday"));
        } else {
            // 否则按单个工作日拼接
            for (PeriodType periodType : new PeriodType[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}) {
                if ((type & periodType.getCode()) != 0) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(localeProvider.apply(periodType.getNameKey()));
                }
            }
        }

        // 处理周末
        int selectedWeekend = type & weekendMask;
        if (selectedWeekend == weekendMask) {
            // 周六+周日都选
            if (sb.length() > 0) sb.append(", ");
            sb.append(localeProvider.apply("label.server.license.weekend"));
        } else if (selectedWeekend != 0) {
            // 只选周六或周日
            for (PeriodType periodType : new PeriodType[]{SATURDAY, SUNDAY}) {
                if ((selectedWeekend & periodType.getCode()) != 0) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(localeProvider.apply(periodType.getNameKey()));
                }
            }
        }

        return sb.toString();
    }

}

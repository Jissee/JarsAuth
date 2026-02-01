package me.jissee.jarsauth.data.model;

import me.jissee.jarsauth.data.service.ServerLicenseService;
import me.jissee.jarsauth.gui.Locales;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public record ServerLicense(
        String id,
        LocalDate validFrom,
        LocalDate validUntil,
        int type,
        LocalTime resetTime,
        LocalTime clearTime,
        long allowance
) implements Comparable<ServerLicense> {
    public static ServerLicense getDefault(ServerLicenseService service) {
        return  new ServerLicense(
                service.getNextAvailableId(),
                LocalDate.now(),
                LocalDate.now(),
                0,
                LocalTime.of(0,0,0),
                LocalTime.of(0,0,0),
                0
        );
    }

    public String toStringFormatted() {
        return  "\n" +
                "id=         " + id + "\n" +
                "validUntil= " + validUntil + "\n" +
                "validFrom=  " + validFrom + "\n" +
                "type=       " + PeriodType.parse(type, Locales::getString) + "\n" +
                "resetTime= " + resetTime + "\n" +
                "clearTime= " + clearTime + "\n" +
                "allowance=  " + allowance;
    }

    /* =========================
       有效性判断
       ========================= */
    public boolean isValid(LocalDateTime now) {
        // 周期型
        if (type != 0) {
            LocalDate today = now.toLocalDate();
            if (today.isBefore(validFrom) || today.isAfter(validUntil)) {
                return false;
            }

            int dayCode = switch (now.getDayOfWeek()) {
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
        // 单次型
        LocalDateTime from = LocalDateTime.of(validFrom, resetTime);
        LocalDateTime until = LocalDateTime.of(validUntil, clearTime);

        return !now.isBefore(from) && !now.isAfter(until);
    }

    /* =========================
       排序逻辑（纯比较）
       ========================= */
    @Override
    public int compareTo(ServerLicense other) {
        boolean thisPeriodic = this.type != 0;
        boolean otherPeriodic = other.type != 0;

        if (thisPeriodic != otherPeriodic) {
            return thisPeriodic ? -1 : 1;
        }

        if (thisPeriodic) {
            int c = this.clearTime.compareTo(other.clearTime);
            if (c != 0) return c;

            c = this.validUntil.compareTo(other.validUntil);
            if (c != 0) return c;

            return this.id.compareTo(other.id);
        }

        LocalDateTime thisEnd = LocalDateTime.of(validUntil, clearTime);
        LocalDateTime otherEnd = LocalDateTime.of(other.validUntil, other.clearTime);

        int c = thisEnd.compareTo(otherEnd);
        if (c != 0) return c;

        return this.id.compareTo(other.id);
    }

}

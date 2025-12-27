package me.jissee.jarsauth.data.model;

import me.jissee.jarsauth.data.service.ServerLicenseService;
import me.jissee.jarsauth.gui.Locales;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
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
        // 周期型不，
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

        LocalDateTime now = LocalDateTime.now();

        boolean thisPeriodic = this.type != 0;
        boolean otherPeriodic = other.type != 0;

        /* 规则 1：周期型优先 */
        if (thisPeriodic != otherPeriodic) {
            return thisPeriodic ? -1 : 1;
        }

        boolean thisValid = this.isValid(now);
        boolean otherValid = other.isValid(now);

        /* 规则 2：有效优先 */
        if (thisValid != otherValid) {
            return thisValid ? -1 : 1;
        }

    /* =========================
       周期型对象比较
       ========================= */
        if (thisPeriodic) {

            int clearCompare = this.clearTime.compareTo(other.clearTime);
            if (clearCompare != 0) {
                return clearCompare;
            }

            int untilCompare = this.validUntil.compareTo(other.validUntil);
            if (untilCompare != 0) {
                return untilCompare;
            }

            /* 兜底：licenseId */
            return this.id.compareTo(other.id);
        }

    /* =========================
       单次型对象比较
       ========================= */
        LocalDateTime thisEnd =
                LocalDateTime.of(this.validUntil, this.clearTime);
        LocalDateTime otherEnd =
                LocalDateTime.of(other.validUntil, other.clearTime);

        int endCompare = thisEnd.compareTo(otherEnd);
        if (endCompare != 0) {
            return endCompare;
        }

        /* 兜底：licenseId */
        return this.id.compareTo(other.id);
    }

}

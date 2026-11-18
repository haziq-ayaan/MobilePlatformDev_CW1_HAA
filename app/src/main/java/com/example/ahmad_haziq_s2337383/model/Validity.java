// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The <validity> block: when the situation starts and (for roadworks) when it is planned to end.
 * Dates are parsed from ISO-8601 text such as "2026-08-10T11:37:42+01:00".
 */
public class Validity {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    private String status;            // <validityStatus>, e.g. "definedByValidityTimeSpec"
    private OffsetDateTime startTime; // <overallStartTime>
    private OffsetDateTime endTime;   // <overallEndTime> (roadworks only)

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    /** Planned length of the situation, or null if there is no end time. */
    public Duration getDuration() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime);
    }

    /** Planned length in whole days, or -1 if unknown. */
    public long getDurationDays() {
        Duration d = getDuration();
        return d == null ? -1 : d.toDays();
    }

    /** e.g. "3 days", "5 hours", "14 months". */
    public String getReadableDuration() {
        Duration d = getDuration();
        if (d == null || d.isNegative()) {
            return "Unknown";
        }
        long days = d.toDays();
        if (days >= 60) {
            long months = Math.round(days / 30.44);
            return months + " months";
        }
        if (days >= 1) {
            return days + (days == 1 ? " day" : " days");
        }
        long hours = Math.max(1, d.toHours());
        return hours + (hours == 1 ? " hour" : " hours");
    }

    public static String format(OffsetDateTime time) {
        return time == null ? "Not given" : time.format(DISPLAY_FORMAT);
    }
}

// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

/**
 * Groups roadworks by how long they are planned to last, for the colour coding.
 * Bands picked after looking at the sample feed: about 15% last a week or less,
 * 20% up to a month, and most of the rest go on for months.
 */
public enum DurationBand {
    SHORT("Short", "Up to 1 week"),
    MEDIUM("Medium", "Up to 1 month"),
    LONG("Long", "Over 1 month"),
    UNKNOWN("Unknown", "No end date");

    private final String label;
    private final String description;

    DurationBand(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static DurationBand of(Validity validity) {
        long days = validity.getDurationDays();
        if (days < 0) {
            return UNKNOWN;
        }
        if (days <= 7) {
            return SHORT;
        }
        if (days <= 31) {
            return MEDIUM;
        }
        return LONG;
    }
}

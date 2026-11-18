// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

/**
 * The delay a situation causes, taken from the <delaysType> tag inside <impact>.
 * Most roadworks in the feed have no <impact> block at all, so NONE is a normal value.
 */
public enum DelayLevel {
    NONE("No reported delays"),
    DELAYS("Delays"),
    LONG_DELAYS("Long delays"),
    VERY_LONG_DELAYS("Very long delays");

    private final String label;

    DelayLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Converts the feed's text value (e.g. "longDelays") into a DelayLevel. Unknown values become DELAYS. */
    public static DelayLevel fromFeedValue(String value) {
        if (value == null) {
            return NONE;
        }
        switch (value.trim()) {
            case "delays":
                return DELAYS;
            case "longDelays":
                return LONG_DELAYS;
            case "veryLongDelays":
                return VERY_LONG_DELAYS;
            default:
                // A delay type we do not know yet still means some kind of delay
                return DELAYS;
        }
    }
}

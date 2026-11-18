// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

/**
 * Which Traffic Scotland feed a situation came from.
 * The two feeds are downloaded separately, so the category is known before parsing starts.
 */
public enum SituationCategory {
    INCIDENT("Incident"),
    ROADWORKS("Roadworks");

    private final String label;

    SituationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

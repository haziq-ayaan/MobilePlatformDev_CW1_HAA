// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

import java.util.Locale;

/**
 * The kind of situation, used to pick an icon and a label.
 *
 * Incidents are classified from the situationRecord's xsi:type and its sub-type tag
 * (e.g. RoadOrCarriagewayOrLaneManagement + roadClosed).
 * Roadworks all share the type "MaintenanceWorks", so they are classified further
 * from the words in their description (e.g. "TTL" = temporary traffic lights).
 */
public enum SituationType {
    // Unplanned events
    ROAD_CLOSED("Road closed"),
    LANE_RESTRICTION("Lane restriction"),
    QUEUING_TRAFFIC("Queuing traffic"),
    ACCIDENT("Accident"),
    OBSTRUCTION("Obstruction"),
    ROAD_CONDITIONS("Road conditions"),
    INCIDENT_OTHER("Incident"),
    // Roadworks
    ROADWORKS_CLOSURE("Roadworks: closure"),
    ROADWORKS_LANE_CLOSURE("Roadworks: lane closure"),
    ROADWORKS_TRAFFIC_LIGHTS("Roadworks: temporary lights"),
    ROADWORKS_URGENT("Urgent roadworks"),
    ROADWORKS("Roadworks");

    private final String label;

    SituationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRoadworks() {
        return name().startsWith("ROADWORKS");
    }

    /** Works out the type of a fully parsed situation. */
    public static SituationType classify(Situation s) {
        String recordType = s.getRecordType() == null ? "" : s.getRecordType();
        String subType = s.getSubType() == null ? "" : s.getSubType();
        String comment = s.getComment() == null ? "" : s.getComment().toLowerCase(Locale.UK);

        boolean isWorks = s.getCategory() == SituationCategory.ROADWORKS
                || recordType.equals("MaintenanceWorks")
                || recordType.equals("ConstructionWorks");

        if (isWorks) {
            if (Boolean.TRUE.equals(s.getUrgentRoadworks())) {
                return ROADWORKS_URGENT;
            }
            if (comment.matches(".*\\bttl.*") || comment.contains("traffic light")
                    || comment.contains("temp light") || comment.contains("portable")) {
                return ROADWORKS_TRAFFIC_LIGHTS;
            }
            if (comment.contains("lane closure") || comment.matches(".*\\bmlc.*")
                    || comment.contains("hard shoulder") || comment.contains("contraflow")
                    || comment.contains("lane restriction")) {
                return ROADWORKS_LANE_CLOSURE;
            }
            if (comment.contains("closure") || comment.contains("closed")) {
                return ROADWORKS_CLOSURE;
            }
            return ROADWORKS;
        }

        switch (recordType) {
            case "RoadOrCarriagewayOrLaneManagement":
                if (subType.equals("roadClosed") || subType.equals("carriagewayClosures")
                        || subType.equals("rampClosures")) {
                    return ROAD_CLOSED;
                }
                return LANE_RESTRICTION;
            case "AbnormalTraffic":
                return QUEUING_TRAFFIC;
            case "Accident":
                return ACCIDENT;
            case "VehicleObstruction":
            case "GeneralObstruction":
            case "AnimalPresenceObstruction":
            case "EnvironmentalObstruction":
            case "InfrastructureDamageObstruction":
                return OBSTRUCTION;
            case "PoorEnvironmentConditions":
            case "WeatherRelatedRoadConditions":
            case "NonWeatherRelatedRoadConditions":
                return ROAD_CONDITIONS;
            default:
                return INCIDENT_OTHER;
        }
    }
}

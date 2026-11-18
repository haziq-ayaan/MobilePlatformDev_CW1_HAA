// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One end of a situation's location: the <from> or the <to> point.
 * Incidents use junction points (<name>, <ilc>, <otherName>);
 * roadworks use non-junction points (<name> with linkName / nonLinkedPointName).
 */
public class LocationPoint {
    private double latitude;
    private double longitude;
    private boolean hasCoordinates = false;

    private String pointType;        // xsi:type, e.g. "TpegJunction" or "TpegNonJunctionPoint"
    private String junctionName;     // e.g. "J19"
    private String linkName;         // e.g. "M8" (roadworks)
    private String description;      // e.g. "M8 J18 Charing Cross"
    private final List<String> roadNumbers = new ArrayList<>(); // e.g. "A90", "B957"

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        updateHasCoordinates();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        updateHasCoordinates();
    }

    private void updateHasCoordinates() {
        // 0,0 is in the Atlantic, so it means "not set" for Scottish data
        hasCoordinates = latitude != 0 && longitude != 0;
    }

    public boolean hasCoordinates() {
        return hasCoordinates;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public String getJunctionName() {
        return junctionName;
    }

    public String getLinkName() {
        return linkName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getRoadNumbers() {
        return roadNumbers;
    }

    /**
     * Stores one descriptor block read by the parser.
     *
     * @param element        the wrapping tag: "name", "ilc" or "otherName"
     * @param descriptorType the value of the matching ...DescriptorType tag (may be null)
     * @param value          the text inside <value lang="en">
     */
    public void addDescriptor(String element, String descriptorType, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        value = value.trim();

        if ("ilc".equals(element)) {
            // ILC descriptors hold road numbers (tpegIlcName1 is the main road)
            if (!roadNumbers.contains(value)) {
                roadNumbers.add(value);
            }
        } else if ("name".equals(element)) {
            if ("junctionName".equals(descriptorType)) {
                junctionName = value;
            } else if ("linkName".equals(descriptorType)) {
                linkName = value;
                if (!roadNumbers.contains(value)) {
                    roadNumbers.add(value);
                }
            } else {
                // nonLinkedPointName and anything new: a readable place description
                description = value;
            }
        } else if ("otherName".equals(element)) {
            description = value; // intersectionName, e.g. "A90 Brechin"
        }
    }

    /** A short readable name for this point, using whichever descriptor is available. */
    public String getDisplayName() {
        if (description != null) {
            return description;
        }
        if (junctionName != null && !roadNumbers.isEmpty()) {
            return roadNumbers.get(0) + " " + junctionName;
        }
        if (junctionName != null) {
            return junctionName;
        }
        if (linkName != null) {
            return linkName;
        }
        return "Unnamed point";
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + latitude + ", " + longitude + ")";
    }
}

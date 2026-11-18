// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

/**
 * The <groupOfLocations> of a situation. In this data it is a line (a road segment)
 * with a "from" point and a "to" point, plus a direction of travel.
 */
public class SituationLocation {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private String locationType;   // xsi:type of groupOfLocations, e.g. "Linear"
    private String direction;      // e.g. "southBound"
    private String segmentType;    // e.g. "segment"
    private LocationPoint from;
    private LocationPoint to;

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(String segmentType) {
        this.segmentType = segmentType;
    }

    public LocationPoint getFrom() {
        return from;
    }

    public void setFrom(LocationPoint from) {
        this.from = from;
    }

    public LocationPoint getTo() {
        return to;
    }

    public void setTo(LocationPoint to) {
        this.to = to;
    }

    public boolean hasCoordinates() {
        return (from != null && from.hasCoordinates()) || (to != null && to.hasCoordinates());
    }

    /** True when both ends exist and are different, so the map should draw a line. */
    public boolean isLine() {
        return from != null && to != null && from.hasCoordinates() && to.hasCoordinates()
                && (from.getLatitude() != to.getLatitude() || from.getLongitude() != to.getLongitude());
    }

    /** Mid-point between from and to, used for the map marker. Returns {lat, lon} or null. */
    public double[] getCentre() {
        if (isLine()) {
            return new double[]{
                    (from.getLatitude() + to.getLatitude()) / 2,
                    (from.getLongitude() + to.getLongitude()) / 2};
        }
        LocationPoint single = (from != null && from.hasCoordinates()) ? from : to;
        if (single != null && single.hasCoordinates()) {
            return new double[]{single.getLatitude(), single.getLongitude()};
        }
        return null;
    }

    /** "southBound" -> "Southbound", "bothWays" -> "Both ways". */
    public String getReadableDirection() {
        if (direction == null || direction.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : direction.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        String text = sb.toString().replace("north bound", "northbound")
                .replace("south bound", "southbound")
                .replace("east bound", "eastbound")
                .replace("west bound", "westbound");
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Distance in km from a given position to the nearest end of this segment.
     * Returns Double.MAX_VALUE if the location has no coordinates.
     */
    public double distanceKmTo(double latitude, double longitude) {
        double best = Double.MAX_VALUE;
        if (from != null && from.hasCoordinates()) {
            best = Math.min(best, haversineKm(latitude, longitude, from.getLatitude(), from.getLongitude()));
        }
        if (to != null && to.hasCoordinates()) {
            best = Math.min(best, haversineKm(latitude, longitude, to.getLatitude(), to.getLongitude()));
        }
        return best;
    }

    /** Great-circle distance between two lat/long points (haversine formula). */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

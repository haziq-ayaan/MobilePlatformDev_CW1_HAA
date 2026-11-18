// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.viewmodel;

import com.example.ahmad_haziq_s2337383.model.Situation;

import java.util.List;

/** The outcome of a road or distance search, kept in the ViewModel so it survives rotation. */
public class SearchResult {

    public enum Kind {ROAD, DISTANCE}

    private final Kind kind;
    private final String description;
    private final List<Situation> situations;
    // only used for distance searches
    private final double originLat;
    private final double originLon;
    private final double radiusKm;

    public SearchResult(Kind kind, String description, List<Situation> situations,
                        double originLat, double originLon, double radiusKm) {
        this.kind = kind;
        this.description = description;
        this.situations = situations;
        this.originLat = originLat;
        this.originLon = originLon;
        this.radiusKm = radiusKm;
    }

    public Kind getKind() {
        return kind;
    }

    public String getDescription() {
        return description;
    }

    public List<Situation> getSituations() {
        return situations;
    }

    public boolean isDistanceSearch() {
        return kind == Kind.DISTANCE;
    }

    public double getOriginLat() {
        return originLat;
    }

    public double getOriginLon() {
        return originLon;
    }

    public double getRadiusKm() {
        return radiusKm;
    }
}

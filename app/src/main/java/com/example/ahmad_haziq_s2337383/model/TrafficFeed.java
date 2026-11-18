// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** The result of parsing one feed: its publication time and all its situations. */
public class TrafficFeed {
    private final SituationCategory category;
    private final OffsetDateTime publicationTime;
    private final ArrayList<Situation> situations;

    public TrafficFeed(SituationCategory category, OffsetDateTime publicationTime,
                       ArrayList<Situation> situations) {
        this.category = category;
        this.publicationTime = publicationTime;
        this.situations = situations;
    }

    public SituationCategory getCategory() {
        return category;
    }

    public OffsetDateTime getPublicationTime() {
        return publicationTime;
    }

    public List<Situation> getSituations() {
        return situations;
    }
}

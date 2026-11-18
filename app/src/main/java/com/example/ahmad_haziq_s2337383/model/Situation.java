// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

import com.example.ahmad_haziq_s2337383.data.CommentParser;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One <situation> from either feed (an unplanned incident or a roadworks entry).
 * Filled in by TrafficXmlParser; finishParsing() then works out the derived fields.
 */
public class Situation {

    private final SituationCategory category;

    // <situation id=".." version="..">
    private String situationId;
    // <headerInformation>
    private String confidentiality;
    private String informationStatus;
    // <situationRecord xsi:type=".." id=".." version="..">
    private String recordType;
    private String recordId;
    private String recordVersion;
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private String probability;
    private String sourceName;
    private String sourceType;
    private String complianceOption;
    private String subType;            // e.g. roadClosed, queuingTraffic, roadworks
    private Boolean urgentRoadworks;   // roadworks only
    private String comment;            // <generalPublicComment> text

    private final Validity validity = new Validity();
    private final Impact impact = new Impact();
    private final SituationLocation location = new SituationLocation();

    // Any tags we do not recognise yet are kept here rather than lost (the feed can change)
    private final Map<String, String> otherDetails = new LinkedHashMap<>();

    // Derived in finishParsing()
    private SituationType type = SituationType.INCIDENT_OTHER;
    private final List<String> roads = new ArrayList<>();
    private String directionText;
    private String worksDescription;

    public Situation(SituationCategory category) {
        this.category = category;
    }

    /** Called once the closing </situation> tag is reached, to work out derived fields. */
    public void finishParsing() {
        roads.clear();
        // Road numbers from the location points come first (they are the most reliable)
        addRoads(location.getFrom());
        addRoads(location.getTo());
        // then any extra roads mentioned in the comment text
        for (String road : CommentParser.extractRoadNumbers(comment)) {
            if (!roads.contains(road)) {
                roads.add(road);
            }
        }

        directionText = location.getReadableDirection();
        if (directionText == null) {
            directionText = CommentParser.extractDirection(comment);
        }

        if (category == SituationCategory.ROADWORKS) {
            worksDescription = CommentParser.extractWorksDescription(comment);
        }

        type = SituationType.classify(this);
    }

    private void addRoads(LocationPoint point) {
        if (point == null) {
            return;
        }
        for (String road : point.getRoadNumbers()) {
            if (CommentParser.isRoadNumber(road)) {
                String normalised = CommentParser.normaliseRoad(road);
                if (!roads.contains(normalised)) {
                    roads.add(normalised);
                }
            }
        }
    }

    // ---------- search helpers ----------

    /**
     * True if this situation is on the given road.
     * Road numbers ("M8", "a9") must match exactly, so "A9" does not match "A90".
     * Anything else (e.g. "Kingston Bridge") is matched as text in the comment and place names.
     */
    public boolean matchesRoad(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        String q = query.trim();
        if (CommentParser.isRoadNumber(q)) {
            return roads.contains(CommentParser.normaliseRoad(q));
        }
        String lower = q.toLowerCase(Locale.UK);
        if (comment != null && comment.toLowerCase(Locale.UK).contains(lower)) {
            return true;
        }
        return nameContains(location.getFrom(), lower) || nameContains(location.getTo(), lower);
    }

    private boolean nameContains(LocationPoint p, String lowerQuery) {
        return p != null && p.getDisplayName().toLowerCase(Locale.UK).contains(lowerQuery);
    }

    /** Distance in km from a position to the nearest end of this situation. */
    public double distanceKmTo(double latitude, double longitude) {
        return location.distanceKmTo(latitude, longitude);
    }

    // ---------- display helpers ----------

    /** Main road for the list item, e.g. "M8", or the category name if none is known. */
    public String getPrimaryRoad() {
        return roads.isEmpty() ? category.getLabel() : roads.get(0);
    }

    public DelayLevel getDelayLevel() {
        return impact.getDelayLevel();
    }

    // ---------- getters and setters ----------

    public SituationCategory getCategory() {
        return category;
    }

    public String getSituationId() {
        return situationId;
    }

    public void setSituationId(String situationId) {
        this.situationId = situationId;
    }

    public String getConfidentiality() {
        return confidentiality;
    }

    public void setConfidentiality(String confidentiality) {
        this.confidentiality = confidentiality;
    }

    public String getInformationStatus() {
        return informationStatus;
    }

    public void setInformationStatus(String informationStatus) {
        this.informationStatus = informationStatus;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordVersion() {
        return recordVersion;
    }

    public void setRecordVersion(String recordVersion) {
        this.recordVersion = recordVersion;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public OffsetDateTime getVersionTime() {
        return versionTime;
    }

    public void setVersionTime(OffsetDateTime versionTime) {
        this.versionTime = versionTime;
    }

    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getComplianceOption() {
        return complianceOption;
    }

    public void setComplianceOption(String complianceOption) {
        this.complianceOption = complianceOption;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public Boolean getUrgentRoadworks() {
        return urgentRoadworks;
    }

    public void setUrgentRoadworks(Boolean urgentRoadworks) {
        this.urgentRoadworks = urgentRoadworks;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Validity getValidity() {
        return validity;
    }

    public Impact getImpact() {
        return impact;
    }

    public SituationLocation getLocation() {
        return location;
    }

    public Map<String, String> getOtherDetails() {
        return otherDetails;
    }

    public void addOtherDetail(String key, String value) {
        otherDetails.put(key, value);
    }

    public SituationType getType() {
        return type;
    }

    public List<String> getRoads() {
        return roads;
    }

    public String getDirectionText() {
        return directionText;
    }

    public String getWorksDescription() {
        return worksDescription;
    }

    @Override
    public String toString() {
        return "Situation{" + situationId
                + ", " + type.getLabel()
                + ", roads=" + roads
                + ", dir=" + directionText
                + ", impact=" + impact.getSummary()
                + ", start=" + validity.getStartTime()
                + ", end=" + validity.getEndTime()
                + ", from=" + location.getFrom()
                + ", to=" + location.getTo()
                + (worksDescription != null ? ", works=" + worksDescription : "")
                + ", comment='" + comment + "'}";
    }
}

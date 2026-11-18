// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import android.util.Log;

import com.example.ahmad_haziq_s2337383.model.DelayLevel;
import com.example.ahmad_haziq_s2337383.model.LocationPoint;
import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.model.TrafficFeed;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

/**
 * Parses a Traffic Scotland DATEX II XML String with XmlPullParser.
 *
 * How it works: PullParser gives us a stream of events (START_TAG, TEXT, END_TAG).
 * We keep a "path" list of the tags we are currently inside, e.g.
 *   [... situationRecord, groupOfLocations, tpegLinearLocation, from, pointCoordinates, latitude]
 * so that when a TEXT event arrives we know exactly which field it belongs to,
 * even when the same tag name (like <value> or <name>) appears in several places.
 *
 * Create a new TrafficXmlParser for each parse (it keeps state while parsing).
 */
public class TrafficXmlParser {
    private static final String TAG = "TrafficXmlParser";

    // State used while parsing
    private final ArrayList<String> path = new ArrayList<>();
    private Situation currentSituation;
    private LocationPoint currentPoint;
    private String descriptorValue;   // text of <value> inside <name>/<ilc>/<otherName>
    private String descriptorType;    // text of the ...DescriptorType tag next to it
    private OffsetDateTime publicationTime;

    /**
     * Parses the downloaded XML String into a TrafficFeed.
     *
     * @param xml      the XML text exactly as downloaded
     * @param category which feed this is (incidents or roadworks)
     */
    public TrafficFeed parse(String xml, SituationCategory category)
            throws XmlPullParserException, IOException {

        ArrayList<Situation> situations = new ArrayList<>();
        path.clear();
        currentSituation = null;
        currentPoint = null;
        publicationTime = null;

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true); // so getName() returns "type", not "xsi:type"
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(cleanXml(xml)));

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                path.add(tag);
                handleStartTag(xpp, tag, category);

            } else if (eventType == XmlPullParser.TEXT) {
                String text = xpp.getText();
                if (text != null && !text.trim().isEmpty() && !path.isEmpty()) {
                    handleText(text.trim());
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                String tag = xpp.getName();
                Situation finished = handleEndTag(tag);
                if (finished != null) {
                    situations.add(finished);
                }
                if (!path.isEmpty()) {
                    path.remove(path.size() - 1);
                }
            }

            eventType = xpp.next();
        }

        Log.d(TAG, "Parsed " + situations.size() + " " + category.getLabel() + " situations");
        return new TrafficFeed(category, publicationTime, situations);
    }

    /** Removes anything before the XML declaration (the starter code noted leading garbage). */
    private String cleanXml(String xml) throws XmlPullParserException {
        if (xml == null) {
            throw new XmlPullParserException("No XML data to parse");
        }
        int start = xml.indexOf("<?");
        if (start < 0) {
            start = xml.indexOf('<');
        }
        if (start < 0) {
            throw new XmlPullParserException("Downloaded data does not contain XML");
        }
        return xml.substring(start);
    }

    // ------------------------------------------------------------------ START_TAG

    private void handleStartTag(XmlPullParser xpp, String tag, SituationCategory category) {
        switch (tag) {
            case "situation":
                currentSituation = new Situation(category);
                currentSituation.setSituationId(getAttribute(xpp, "id"));
                break;

            case "situationRecord":
                if (currentSituation != null) {
                    // e.g. xsi:type="RoadOrCarriagewayOrLaneManagement"
                    currentSituation.setRecordType(getAttribute(xpp, "type"));
                    currentSituation.setRecordId(getAttribute(xpp, "id"));
                    currentSituation.setRecordVersion(getAttribute(xpp, "version"));
                }
                break;

            case "groupOfLocations":
                if (currentSituation != null) {
                    currentSituation.getLocation().setLocationType(getAttribute(xpp, "type"));
                }
                break;

            case "from":
            case "to":
                if (currentSituation != null) {
                    currentPoint = new LocationPoint();
                    currentPoint.setPointType(getAttribute(xpp, "type"));
                }
                break;

            case "name":
            case "ilc":
            case "otherName":
                // a new descriptor block starts inside a point
                descriptorValue = null;
                descriptorType = null;
                break;

            default:
                break;
        }
    }

    /** Reads an attribute by its local name (ignores any namespace prefix such as xsi:). */
    private String getAttribute(XmlPullParser xpp, String localName) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (localName.equals(xpp.getAttributeName(i))) {
                return xpp.getAttributeValue(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ TEXT

    private void handleText(String text) {
        String tag = path.get(path.size() - 1);
        String parent = path.size() > 1 ? path.get(path.size() - 2) : "";

        if (currentSituation == null) {
            // Outside any situation: only the feed's publication time is useful
            if ("publicationTime".equals(tag)) {
                publicationTime = parseTime(text);
            }
            return;
        }
        Situation s = currentSituation;

        switch (tag) {
            // header
            case "confidentiality":
                s.setConfidentiality(text);
                break;
            case "informationStatus":
                s.setInformationStatus(text);
                break;

            // record details
            case "situationRecordCreationTime":
                s.setCreationTime(parseTime(text));
                break;
            case "situationRecordVersionTime":
                s.setVersionTime(parseTime(text));
                break;
            case "probabilityOfOccurrence":
                s.setProbability(text);
                break;
            case "sourceIdentification":
                if (s.getSourceName() == null) s.setSourceName(text);
                break;
            case "sourceType":
                s.setSourceType(text);
                break;
            case "complianceOption":
                s.setComplianceOption(text);
                break;
            case "urgentRoadworks":
                s.setUrgentRoadworks(Boolean.parseBoolean(text));
                break;

            // validity
            case "validityStatus":
                s.getValidity().setStatus(text);
                break;
            case "overallStartTime":
                s.getValidity().setStartTime(parseTime(text));
                break;
            case "overallEndTime":
                s.getValidity().setEndTime(parseTime(text));
                break;

            // impact
            case "numberOfOperationalLanes":
                s.getImpact().setOperationalLanes(parseInt(text));
                break;
            case "numberOfLanesRestricted":
                s.getImpact().setLanesRestricted(parseInt(text));
                break;
            case "delaysType":
                s.getImpact().setDelayLevel(DelayLevel.fromFeedValue(text));
                break;

            // location
            case "tpegDirection":
                s.getLocation().setDirection(text);
                break;
            case "tpegLinearLocationType":
                s.getLocation().setSegmentType(text);
                break;
            case "latitude":
                if (currentPoint != null) currentPoint.setLatitude(parseDouble(text));
                break;
            case "longitude":
                if (currentPoint != null) currentPoint.setLongitude(parseDouble(text));
                break;
            case "tpegJunctionPointDescriptorType":
            case "tpegIlcPointDescriptorType":
            case "tpegOtherPointDescriptorType":
                descriptorType = text;
                break;

            // <value lang="en"> appears in several places, so check where we are
            case "value":
                handleValue(text);
                break;

            // known tags we deliberately skip (duplicates of data we already keep)
            case "situationRecordCreationReference":
            case "situationRecordFirstSupplierVersionTime":
                break;

            default:
                if ("situationRecord".equals(parent) && tag.endsWith("Type")) {
                    // The sub-type tag: roadOrCarriagewayOrLaneManagementType, abnormalTrafficType,
                    // roadMaintenanceType, or a new one such as accidentType
                    if (s.getSubType() == null) s.setSubType(text);
                } else {
                    // Unknown tag: keep it so new feed fields still appear, and never crash
                    s.addOtherDetail(toReadableName(tag), text);
                }
                break;
        }
    }

    private void handleValue(String text) {
        if (path.contains("generalPublicComment")) {
            String existing = currentSituation.getComment();
            currentSituation.setComment(existing == null ? text : existing + " " + text);
        } else if (path.contains("sourceName")) {
            currentSituation.setSourceName(text);
        } else if (currentPoint != null) {
            descriptorValue = text; // stored when </name>, </ilc> or </otherName> is reached
        }
    }

    // ------------------------------------------------------------------ END_TAG

    /** Returns the finished Situation when </situation> is reached, otherwise null. */
    private Situation handleEndTag(String tag) {
        switch (tag) {
            case "name":
            case "ilc":
            case "otherName":
                if (currentPoint != null) {
                    currentPoint.addDescriptor(tag, descriptorType, descriptorValue);
                }
                descriptorValue = null;
                descriptorType = null;
                break;

            case "from":
                if (currentSituation != null) currentSituation.getLocation().setFrom(currentPoint);
                currentPoint = null;
                break;

            case "to":
                if (currentSituation != null) currentSituation.getLocation().setTo(currentPoint);
                currentPoint = null;
                break;

            case "situation":
                if (currentSituation != null) {
                    Situation finished = currentSituation;
                    finished.finishParsing();
                    Log.d(TAG, finished.toString());
                    currentSituation = null;
                    return finished;
                }
                break;

            default:
                break;
        }
        return null;
    }

    // ------------------------------------------------------------------ conversions

    private OffsetDateTime parseTime(String text) {
        try {
            return OffsetDateTime.parse(text);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Bad date: " + text);
            return null;
        }
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Bad number: " + text);
            return null;
        }
    }

    private double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Bad coordinate: " + text);
            return 0;
        }
    }

    /** "carriagewayUsed" -> "Carriageway used" */
    private String toReadableName(String tag) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tag.length(); i++) {
            char c = tag.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Breaks down the free text of a <generalPublicComment> into useful parts using String methods.
 *
 * Example roadworks comment:  "A702 - NB - South of Coulter - Temp Lights"
 *   roads       -> [A702]
 *   direction   -> Northbound
 *   works       -> Temporary traffic lights
 */
public final class CommentParser {

    // A, M or B road number, optionally followed by "(M)" or "M" for motorway sections,
    // e.g. "A9", "M8", "B957", "A74(M)", "A74M"
    private static final Pattern ROAD_PATTERN =
            Pattern.compile("\\b([AMB])\\s?(\\d{1,4})(\\s?\\(M\\)|M)?(?![0-9])");

    private CommentParser() {
        // utility class: static methods only
    }

    /** Converts a road number to one standard spelling, e.g. "a74 (m)" -> "A74(M)". */
    public static String normaliseRoad(String road) {
        if (road == null) {
            return null;
        }
        String r = road.toUpperCase(Locale.UK).replace(" ", "");
        Matcher m = ROAD_PATTERN.matcher(r);
        if (m.find()) {
            String result = m.group(1) + m.group(2);
            if (m.group(3) != null) {
                result += "(M)";
            }
            return result;
        }
        return r;
    }

    /** True if the text looks like a road number such as "M8" or "A82". */
    public static boolean isRoadNumber(String text) {
        if (text == null) {
            return false;
        }
        return text.trim().toUpperCase(Locale.UK).matches("[AMB]\\s?\\d{1,4}\\s?(\\(M\\)|M)?");
    }

    /** All road numbers mentioned in the text, in order, without duplicates. */
    public static List<String> extractRoadNumbers(String text) {
        List<String> roads = new ArrayList<>();
        if (text == null) {
            return roads;
        }
        Matcher m = ROAD_PATTERN.matcher(text.toUpperCase(Locale.UK));
        while (m.find()) {
            String road = normaliseRoad(m.group());
            if (!roads.contains(road)) {
                roads.add(road);
            }
        }
        return roads;
    }

    /** Finds the direction of travel written in the text, e.g. "NB" or "Northbound". */
    public static String extractDirection(String text) {
        if (text == null) {
            return null;
        }
        String t = " " + text.toLowerCase(Locale.UK).replace(".", " ").replace(",", " ") + " ";
        if (t.contains("both direction") || t.contains("both ways")) return "Both directions";
        if (t.contains("northbound") || t.contains(" nb ")) return "Northbound";
        if (t.contains("southbound") || t.contains(" sb ")) return "Southbound";
        if (t.contains("eastbound") || t.contains(" eb ")) return "Eastbound";
        if (t.contains("westbound") || t.contains(" wb ")) return "Westbound";
        return null;
    }

    /**
     * Roadworks comments are usually written as "road - place - direction - works",
     * separated by " - ". The last part describes the works. Returns null if the
     * comment is not in that format.
     */
    public static String extractWorksDescription(String text) {
        if (text == null || !text.contains(" - ")) {
            return null;
        }
        String[] parts = text.split(" - ");
        String last = parts[parts.length - 1].trim();
        if (last.isEmpty()) {
            return null;
        }
        return expandAbbreviations(last);
    }

    /** Replaces the abbreviations Traffic Scotland uses with plain English. */
    public static String expandAbbreviations(String text) {
        if (text == null) {
            return null;
        }
        String result = text
                .replaceAll("(?i)\\bTTL'?s\\b", "temporary traffic lights")
                .replaceAll("(?i)\\bTTL\\b", "temporary traffic lights")
                .replaceAll("(?i)\\bMLC'?s\\b", "mobile lane closures")
                .replaceAll("(?i)\\bMLC\\b", "mobile lane closure")
                .replaceAll("(?i)\\bMVMS\\b", "mobile variable message sign")
                .replaceAll("(?i)\\bTemp\\b", "temporary")
                .replaceAll("(?i)\\bRbt\\b", "roundabout")
                .replaceAll("(?i)\\bJct\\b", "junction")
                .replaceAll("(?i)\\bVergeworks\\b", "verge works")
                .trim();
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.isEmpty()) {
            return result;
        }
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }
}

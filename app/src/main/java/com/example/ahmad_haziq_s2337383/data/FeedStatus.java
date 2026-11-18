// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * What the UI should tell the user about the data: is it loading, live, an offline copy, or missing?
 */
public class FeedStatus {

    public enum State {
        LOADING,   // a download is in progress
        LIVE,      // both feeds freshly downloaded
        OFFLINE,   // showing saved data because the download failed
        ERROR      // no data available at all for at least one feed
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK).withZone(ZoneId.systemDefault());

    private final State state;
    private final long dataTimeMillis; // when the data shown was downloaded (0 = unknown)
    private final String message;

    public FeedStatus(State state, long dataTimeMillis, String message) {
        this.state = state;
        this.dataTimeMillis = dataTimeMillis;
        this.message = message;
    }

    public State getState() {
        return state;
    }

    public long getDataTimeMillis() {
        return dataTimeMillis;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLoading() {
        return state == State.LOADING;
    }

    /** Text for a status bar, e.g. "Offline – showing data from 18 Nov, 14:05". */
    public String getDisplayText() {
        String time = dataTimeMillis > 0 ? TIME_FORMAT.format(Instant.ofEpochMilli(dataTimeMillis)) : null;
        switch (state) {
            case LOADING:
                return "Updating…";
            case LIVE:
                return "Live data · updated " + time;
            case OFFLINE:
                return "Offline – showing data from " + time
                        + (message != null ? " (" + message + ")" : "");
            case ERROR:
            default:
                return message != null ? message : "No data available";
        }
    }
}

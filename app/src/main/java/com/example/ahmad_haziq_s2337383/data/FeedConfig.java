// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

/** Fixed settings for downloading the Traffic Scotland feeds. */
public final class FeedConfig {

    /** Unplanned events (incidents) */
    public static final String INCIDENTS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    /**
     * Current roadworks.
     */
    public static final String ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    /**
     * How often the data is downloaded again automatically.
     * The brief asks for every hour (60); ensure I use a small value such as 2 when recording the demo video.
     */
    public static final long AUTO_UPDATE_MINUTES = 60;

    /**
     * DEVELOPMENT ONLY: when the live feed and the saved copy both fail, load the sample XML files
     * bundled in app/src/main/assets so the interface can still be built and tested.
     */
    public static final boolean USE_SAMPLE_DATA_FALLBACK = false;
    public static final String SAMPLE_INCIDENTS_ASSET = "sample_unplanned.xml";
    public static final String SAMPLE_ROADWORKS_ASSET = "sample_roadworks.xml";

    /** Network timeouts, so a bad connection fails quickly instead of hanging. */
    public static final int CONNECT_TIMEOUT_MS = 15000;
    public static final int READ_TIMEOUT_MS = 30000;

    private FeedConfig() {
    }
}

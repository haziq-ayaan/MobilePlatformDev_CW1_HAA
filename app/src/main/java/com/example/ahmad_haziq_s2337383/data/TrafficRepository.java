// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.model.TrafficFeed;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The "Model" side of MVVM: the single place that fetches, parses and caches the traffic data.
 *
 * Threading (replaces the starter code's new Thread + runOnUiThread):
 *  - downloading and parsing run on a background ExecutorService, never on the UI thread;
 *  - results are handed to the UI with LiveData.postValue(), which is safe to call from any thread;
 *    the UI observes the LiveData and is updated on the main thread automatically.
 *
 * Auto-update: a ScheduledExecutorService refreshes immediately at start-up and then every
 * FeedConfig.AUTO_UPDATE_MINUTES.
 */
public class TrafficRepository {
    private static final String TAG = "TrafficRepository";

    private final Context appContext;
    private final TrafficApi api;
    private final FeedCache cache;

    private final ExecutorService workExecutor = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService updateScheduler;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private final MutableLiveData<List<Situation>> incidents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Situation>> roadworks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<FeedStatus> status =
            new MutableLiveData<>(new FeedStatus(FeedStatus.State.LOADING, 0, null));

    public TrafficRepository(Context context) {
        appContext = context.getApplicationContext();
        api = new TrafficApi(new ApiKeys(appContext));
        cache = new FeedCache(appContext);
    }

    // ------------------------------------------------------------------ data for the ViewModel

    public LiveData<List<Situation>> getIncidents() {
        return incidents;
    }

    public LiveData<List<Situation>> getRoadworks() {
        return roadworks;
    }

    public LiveData<FeedStatus> getStatus() {
        return status;
    }

    // ------------------------------------------------------------------ updating

    /** Starts downloading now, then repeats every intervalMinutes. Safe to call more than once. */
    public void startAutoUpdate(long intervalMinutes) {
        if (updateScheduler != null) {
            return;
        }
        updateScheduler = Executors.newSingleThreadScheduledExecutor();
        updateScheduler.scheduleWithFixedDelay(this::refresh, 0, intervalMinutes, TimeUnit.MINUTES);
        Log.d(TAG, "Auto-update every " + intervalMinutes + " min");
    }

    /** Requests a refresh. Ignored if one is already running. Can be called from any thread. */
    public void refresh() {
        if (!refreshing.compareAndSet(false, true)) {
            Log.d(TAG, "Refresh already in progress");
            return;
        }
        workExecutor.execute(() -> {
            try {
                refreshFeeds();
            } finally {
                refreshing.set(false);
            }
        });
    }

    /** Stops all background threads. Called when the ViewModel is destroyed. */
    public void shutdown() {
        if (updateScheduler != null) {
            updateScheduler.shutdownNow();
        }
        workExecutor.shutdownNow();
    }

    // ------------------------------------------------------------------ background work

    /** Runs on the background thread. */
    private void refreshFeeds() {
        FeedStatus previous = status.getValue();
        status.postValue(new FeedStatus(FeedStatus.State.LOADING,
                previous == null ? 0 : previous.getDataTimeMillis(), null));

        FeedResult incidentResult = loadFeed(SituationCategory.INCIDENT, FeedConfig.INCIDENTS_URL);
        FeedResult roadworksResult = loadFeed(SituationCategory.ROADWORKS, FeedConfig.ROADWORKS_URL);

        // Only replace what is on screen when we actually got data
        if (incidentResult.feed != null) {
            incidents.postValue(newestFirst(incidentResult.feed.getSituations()));
        }
        if (roadworksResult.feed != null) {
            roadworks.postValue(newestFirst(roadworksResult.feed.getSituations()));
        }
        status.postValue(combineStatus(incidentResult, roadworksResult));
    }

    /** Sorts by start time, most recent first (situations with no start time go last). */
    private List<Situation> newestFirst(List<Situation> list) {
        List<Situation> sorted = new ArrayList<>(list);
        Collections.sort(sorted, (a, b) -> {
            OffsetDateTime ta = a.getValidity().getStartTime();
            OffsetDateTime tb = b.getValidity().getStartTime();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return sorted;
    }

    /** Downloads and parses one feed; if that fails, falls back to the saved copy. */
    private FeedResult loadFeed(SituationCategory category, String url) {
        String problem;
        try {
            String xml = downloadWithRetry(url);
            TrafficFeed feed = new TrafficXmlParser().parse(xml, category);
            cache.save(category, xml); // only saved once we know it parses
            Log.d(TAG, category + ": downloaded " + feed.getSituations().size() + " situations");
            return new FeedResult(feed, false, System.currentTimeMillis(), null);

        } catch (TrafficApi.AuthException e) {
            problem = e.getMessage();
            Log.e(TAG, category + " download refused", e);
        } catch (UnknownHostException | ConnectException e) {
            problem = "no internet connection";
            Log.e(TAG, category + " download failed", e);
        } catch (SocketTimeoutException e) {
            problem = "connection timed out";
            Log.e(TAG, category + " download failed", e);
        } catch (IOException e) {
            // Any other network problem: show what it was (e.g. "HTTP 404", an SSL error)
            problem = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            Log.e(TAG, category + " download failed", e);
        } catch (XmlPullParserException e) {
            problem = "data could not be read";
            Log.e(TAG, category + " parsing failed", e);
        }

        String cachedXml = cache.load(category);
        if (cachedXml != null) {
            try {
                TrafficFeed feed = new TrafficXmlParser().parse(cachedXml, category);
                return new FeedResult(feed, true, cache.savedAt(category), problem);
            } catch (XmlPullParserException | IOException e) {
                Log.e(TAG, "Cached " + category + " data is unreadable", e);
            }
        }

        if (FeedConfig.USE_SAMPLE_DATA_FALLBACK) {
            TrafficFeed sample = loadSampleFeed(category);
            if (sample != null) {
                long time = sample.getPublicationTime() != null
                        ? sample.getPublicationTime().toInstant().toEpochMilli() : 0;
                return new FeedResult(sample, true, time, problem + " – using SAMPLE data");
            }
        }
        return new FeedResult(null, false, 0, problem);
    }

    /** DEVELOPMENT ONLY: parses the sample XML bundled in the assets folder. */
    private TrafficFeed loadSampleFeed(SituationCategory category) {
        String asset = category == SituationCategory.INCIDENT
                ? FeedConfig.SAMPLE_INCIDENTS_ASSET : FeedConfig.SAMPLE_ROADWORKS_ASSET;
        try (InputStream in = appContext.getAssets().open(asset)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, n);
            }
            String xml = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            Log.w(TAG, "Using SAMPLE " + category + " data from assets/" + asset);
            return new TrafficXmlParser().parse(xml, category);
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Could not load sample " + category + " data", e);
            return null;
        }
    }

    /** Network hiccups happen on mobile, so a failed download is tried once more after 2 seconds. */
    private String downloadWithRetry(String url) throws IOException {
        try {
            return api.download(url);
        } catch (TrafficApi.AuthException e) {
            throw e; // wrong keys will not fix themselves
        } catch (IOException first) {
            Log.w(TAG, "Download failed, retrying once: " + first.getMessage());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw first;
            }
            return api.download(url);
        }
    }

    private FeedStatus combineStatus(FeedResult a, FeedResult b) {
        if (a.feed == null || b.feed == null) {
            StringBuilder problems = new StringBuilder();
            if (a.feed == null) problems.append("Incidents: ").append(a.problem);
            if (b.feed == null) {
                if (problems.length() > 0) problems.append("\n");
                problems.append("Roadworks: ").append(b.problem);
            }
            return new FeedStatus(FeedStatus.State.ERROR, 0,
                    "No data available. Will retry automatically.\n" + problems);
        }
        long oldest = Math.min(a.timeMillis, b.timeMillis);
        if (a.fromCache || b.fromCache) {
            return new FeedStatus(FeedStatus.State.OFFLINE, oldest, a.problem != null ? a.problem : b.problem);
        }
        return new FeedStatus(FeedStatus.State.LIVE, oldest, null);
    }

    /** The outcome of loading one feed. */
    private static class FeedResult {
        final TrafficFeed feed;     // null if no data at all
        final boolean fromCache;
        final long timeMillis;
        final String problem;

        FeedResult(TrafficFeed feed, boolean fromCache, long timeMillis, String problem) {
            this.feed = feed;
            this.fromCache = fromCache;
            this.timeMillis = timeMillis;
            this.problem = problem;
        }
    }
}

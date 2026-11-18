// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import android.content.Context;
import android.util.Log;

import com.example.ahmad_haziq_s2337383.model.SituationCategory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Keeps a copy of the last XML successfully downloaded for each feed, in the app's private storage.
 * When there is no internet connection the app parses this copy instead,
 * so the user still sees the most recent data (and is told how old it is).
 */
public class FeedCache {
    private static final String TAG = "FeedCache";
    private final File directory;

    public FeedCache(Context context) {
        directory = context.getFilesDir();
    }

    private File fileFor(SituationCategory category) {
        return new File(directory, "feed_" + category.name().toLowerCase(Locale.UK) + ".xml");
    }

    public void save(SituationCategory category, String xml) {
        try {
            Files.write(fileFor(category).toPath(), xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Could not save " + category + " cache", e);
        }
    }

    /** The cached XML, or null if nothing has been saved yet. */
    public String load(SituationCategory category) {
        File file = fileFor(category);
        if (!file.exists()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Could not read " + category + " cache", e);
            return null;
        }
    }

    /** When the cached copy was saved (milliseconds since 1970), or 0 if there is none. */
    public long savedAt(SituationCategory category) {
        File file = fileFor(category);
        return file.exists() ? file.lastModified() : 0;
    }
}

// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Reads the DATEX II Client ID and Key from the app's manifest meta-data.
 *
 * This follows the module's "Protecting your API keys" guide:
 *   local.properties  ->  secrets-gradle-plugin  ->  <meta-data> in AndroidManifest.xml  ->  this class
 * so the real keys are never written in the source code or uploaded to GitHub.
 */
public class ApiKeys {
    private static final String TAG = "ApiKeys";
    private static final String CLIENT_ID_KEY = "com.example.ahmad_haziq_s2337383.DATEX_CLIENT_ID";
    private static final String CLIENT_KEY_KEY = "com.example.ahmad_haziq_s2337383.DATEX_CLIENT_KEY";

    private final String clientId;
    private final String clientKey;

    public ApiKeys(Context context) {
        String id = "";
        String key = "";
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = info.metaData;
            if (metaData != null) {
                // get() rather than getString(): a value made only of digits is stored as a number
                id = String.valueOf(metaData.get(CLIENT_ID_KEY));
                key = String.valueOf(metaData.get(CLIENT_KEY_KEY));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not read manifest meta-data", e);
        }
        clientId = id;
        clientKey = key;
        if (!hasRealKeys()) {
            Log.e(TAG, "DATEX II keys missing: add DATEX_CLIENT_ID and DATEX_CLIENT_KEY to local.properties");
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientKey() {
        return clientKey;
    }

    /** False when the placeholder values from local.defaults.properties are still in use. */
    public boolean hasRealKeys() {
        return !clientId.isEmpty() && !clientId.equals("null") && !clientId.startsWith("YOUR_")
                && !clientKey.isEmpty() && !clientKey.equals("null") && !clientKey.startsWith("YOUR_");
    }
}

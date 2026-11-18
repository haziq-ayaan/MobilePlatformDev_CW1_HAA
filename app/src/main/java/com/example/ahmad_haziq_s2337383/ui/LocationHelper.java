// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Gets the phone's coarse (network-based) location using the standard Android LocationManager,
 * so no extra location library is needed. The caller must already have location permission.
 */
public final class LocationHelper {

    public interface Callback {
        void onLocation(@Nullable Location location);
    }

    private LocationHelper() {
    }

    @SuppressLint("MissingPermission") // permission is checked by the fragment before calling this
    public static void getCoarseLocation(Context context, Callback callback) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            callback.onLocation(null);
            return;
        }

        // A recent location the phone already knows about is good enough and is instant
        Location best = null;
        for (String provider : manager.getProviders(true)) {
            Location last = manager.getLastKnownLocation(provider);
            if (last != null && (best == null || last.getTime() > best.getTime())) {
                best = last;
            }
        }
        if (best != null) {
            callback.onLocation(best);
            return;
        }

        // Otherwise ask for one new fix
        String provider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                ? LocationManager.NETWORK_PROVIDER
                : manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : null;
        if (provider == null) {
            callback.onLocation(null);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context), callback::onLocation);
        } else {
            // older Android versions: all four listener methods have to be written out
            manager.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    callback.onLocation(location);
                }

                @Override
                public void onStatusChanged(String p, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(@NonNull String p) {
                }

                @Override
                public void onProviderDisabled(@NonNull String p) {
                    callback.onLocation(null);
                }
            }, Looper.getMainLooper());
        }
    }
}

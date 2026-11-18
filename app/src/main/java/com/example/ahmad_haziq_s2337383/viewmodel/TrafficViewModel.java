// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.ahmad_haziq_s2337383.data.FeedConfig;
import com.example.ahmad_haziq_s2337383.data.FeedStatus;
import com.example.ahmad_haziq_s2337383.data.TrafficRepository;
import com.example.ahmad_haziq_s2337383.model.Situation;

import java.util.List;

/**
 * The "ViewModel" in MVVM. It holds the app's data for the screens (Views),
 * and survives screen rotation, so turning the phone does not reload or lose anything.
 *
 * Search, selection and map state will be added here in the next step.
 */
public class TrafficViewModel extends AndroidViewModel {

    private final TrafficRepository repository;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        // Download at start-up, then automatically at regular intervals
        repository.startAutoUpdate(FeedConfig.AUTO_UPDATE_MINUTES);
    }

    public LiveData<List<Situation>> getIncidents() {
        return repository.getIncidents();
    }

    public LiveData<List<Situation>> getRoadworks() {
        return repository.getRoadworks();
    }

    public LiveData<FeedStatus> getStatus() {
        return repository.getStatus();
    }

    /** Manual refresh (e.g. from a menu button), in addition to the automatic updates. */
    public void refresh() {
        repository.refresh();
    }

    @Override
    protected void onCleared() {
        // The app is closing for good (not just rotating): stop the background threads
        repository.shutdown();
        super.onCleared();
    }
}

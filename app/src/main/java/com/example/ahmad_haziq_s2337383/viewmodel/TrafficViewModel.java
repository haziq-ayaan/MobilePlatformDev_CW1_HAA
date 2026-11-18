// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ahmad_haziq_s2337383.data.FeedConfig;
import com.example.ahmad_haziq_s2337383.data.FeedStatus;
import com.example.ahmad_haziq_s2337383.data.TrafficRepository;
import com.example.ahmad_haziq_s2337383.model.Situation;

import java.util.List;

/**
 * ViewModel shared by MainActivity and all its fragments.
 * It outlives screen rotation, so the selected item, open panel and current tab
 * are all kept when switching between the portrait and landscape layouts.
 */
public class TrafficViewModel extends AndroidViewModel {

    private final TrafficRepository repository;

    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<Boolean> detailOpen = new MutableLiveData<>(false);
    private int currentTabId = 0; // menu item id of the open tab, 0 = not set yet

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        // download at start-up, then again every FeedConfig.AUTO_UPDATE_MINUTES
        repository.startAutoUpdate(FeedConfig.AUTO_UPDATE_MINUTES);
    }

    // ---------- data ----------

    public LiveData<List<Situation>> getIncidents() {
        return repository.getIncidents();
    }

    public LiveData<List<Situation>> getRoadworks() {
        return repository.getRoadworks();
    }

    public LiveData<FeedStatus> getStatus() {
        return repository.getStatus();
    }

    public void refresh() {
        repository.refresh();
    }

    // ---------- selection / detail panel ----------

    public LiveData<Situation> getSelected() {
        return selected;
    }

    public LiveData<Boolean> getDetailOpen() {
        return detailOpen;
    }

    public void selectSituation(Situation situation) {
        selected.setValue(situation);
        detailOpen.setValue(true);
    }

    public void closeDetail() {
        detailOpen.setValue(false);
    }

    // ---------- navigation ----------

    public int getCurrentTabId() {
        return currentTabId;
    }

    public void setCurrentTabId(int tabId) {
        currentTabId = tabId;
    }

    @Override
    protected void onCleared() {
        // app is finishing for good (not just rotating), so stop the background threads
        repository.shutdown();
        super.onCleared();
    }
}

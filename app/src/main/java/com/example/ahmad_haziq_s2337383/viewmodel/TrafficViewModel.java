// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ahmad_haziq_s2337383.data.CommentParser;
import com.example.ahmad_haziq_s2337383.data.FeedConfig;
import com.example.ahmad_haziq_s2337383.data.FeedStatus;
import com.example.ahmad_haziq_s2337383.data.TrafficRepository;
import com.example.ahmad_haziq_s2337383.model.Situation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * ViewModel shared by MainActivity and all its fragments.
 * It outlives screen rotation, so the selection, search results, map view and current tab
 * all stay the same when switching between the portrait and landscape layouts.
 */
public class TrafficViewModel extends AndroidViewModel {

    /** What the map is currently showing. */
    public enum MapMode {ALL, INCIDENTS, ROADWORKS, SEARCH, SELECTED}

    /** Value posted to tabRequest when another screen asks to open the map. */
    public static final String TAB_MAP = "map";

    private final TrafficRepository repository;

    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<Boolean> detailOpen = new MutableLiveData<>(false);
    private final MutableLiveData<MapMode> mapMode = new MutableLiveData<>(MapMode.ALL);
    private final MutableLiveData<String> tabRequest = new MutableLiveData<>();
    private final MediatorLiveData<SearchResult> searchResult = new MediatorLiveData<>();
    private int currentTabId = 0; // menu item id of the open tab, 0 = not set yet

    // the last search, so it can be re-run when the data is refreshed
    private SearchResult.Kind lastKind;
    private String lastRoad;
    private String lastOriginName;
    private double lastLat;
    private double lastLon;
    private double lastRadiusKm;
    private boolean lastIncludeIncidents;
    private boolean lastIncludeRoadworks;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        // download at start-up, then again every FeedConfig.AUTO_UPDATE_MINUTES
        repository.startAutoUpdate(FeedConfig.AUTO_UPDATE_MINUTES);

        // when new data arrives, the current search results are worked out again
        searchResult.addSource(repository.getIncidents(), list -> rerunSearch());
        searchResult.addSource(repository.getRoadworks(), list -> rerunSearch());
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

    // ---------- search ----------

    public LiveData<SearchResult> getSearchResult() {
        return searchResult;
    }

    public void searchByRoad(String road, boolean includeIncidents, boolean includeRoadworks) {
        lastKind = SearchResult.Kind.ROAD;
        lastRoad = road.trim();
        lastIncludeIncidents = includeIncidents;
        lastIncludeRoadworks = includeRoadworks;
        rerunSearch();
    }

    public void searchByDistance(String originName, double lat, double lon, double radiusKm,
                                 boolean includeIncidents, boolean includeRoadworks) {
        lastKind = SearchResult.Kind.DISTANCE;
        lastOriginName = originName;
        lastLat = lat;
        lastLon = lon;
        lastRadiusKm = radiusKm;
        lastIncludeIncidents = includeIncidents;
        lastIncludeRoadworks = includeRoadworks;
        rerunSearch();
    }

    private void rerunSearch() {
        if (lastKind == null) {
            return;
        }
        List<Situation> pool = new ArrayList<>();
        if (lastIncludeIncidents && getIncidents().getValue() != null) {
            pool.addAll(getIncidents().getValue());
        }
        if (lastIncludeRoadworks && getRoadworks().getValue() != null) {
            pool.addAll(getRoadworks().getValue());
        }

        List<Situation> matches = new ArrayList<>();
        String description;
        if (lastKind == SearchResult.Kind.ROAD) {
            for (Situation s : pool) {
                if (s.matchesRoad(lastRoad)) {
                    matches.add(s);
                }
            }
            String what = CommentParser.isRoadNumber(lastRoad)
                    ? "on the " + CommentParser.normaliseRoad(lastRoad)
                    : "matching \"" + lastRoad + "\"";
            description = matches.size() + (matches.size() == 1 ? " result " : " results ") + what;
        } else {
            for (Situation s : pool) {
                if (s.distanceKmTo(lastLat, lastLon) <= lastRadiusKm) {
                    matches.add(s);
                }
            }
            // nearest first
            Collections.sort(matches, Comparator.comparingDouble(s -> s.distanceKmTo(lastLat, lastLon)));
            description = String.format(Locale.UK, "%d %s within %.0f km of %s", matches.size(),
                    matches.size() == 1 ? "result" : "results", lastRadiusKm, lastOriginName);
        }
        searchResult.setValue(new SearchResult(lastKind, description, matches, lastLat, lastLon, lastRadiusKm));
    }

    /** Every road number in the current data, for the search suggestions (motorways first). */
    public List<String> getKnownRoads() {
        TreeSet<String> roads = new TreeSet<>(this::compareRoads);
        addRoads(roads, getIncidents().getValue());
        addRoads(roads, getRoadworks().getValue());
        return new ArrayList<>(roads);
    }

    private void addRoads(TreeSet<String> roads, List<Situation> list) {
        if (list == null) {
            return;
        }
        for (Situation s : list) {
            roads.addAll(s.getRoads());
        }
    }

    /** Sorts M roads, then A, then B, each by number (so A9 comes before A82). */
    private int compareRoads(String a, String b) {
        int letter = Integer.compare("MAB".indexOf(a.charAt(0)), "MAB".indexOf(b.charAt(0)));
        if (letter != 0) {
            return letter;
        }
        int numberCompare = Integer.compare(roadNumber(a), roadNumber(b));
        return numberCompare != 0 ? numberCompare : a.compareTo(b);
    }

    private int roadNumber(String road) {
        String digits = road.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    // ---------- map ----------

    public LiveData<MapMode> getMapMode() {
        return mapMode;
    }

    public void setMapMode(MapMode mode) {
        mapMode.setValue(mode);
    }

    /** The situations the map should draw for the current mode. */
    public List<Situation> getMapSituations() {
        List<Situation> result = new ArrayList<>();
        MapMode mode = mapMode.getValue() == null ? MapMode.ALL : mapMode.getValue();
        switch (mode) {
            case INCIDENTS:
                addAll(result, getIncidents().getValue());
                break;
            case ROADWORKS:
                addAll(result, getRoadworks().getValue());
                break;
            case SEARCH:
                if (searchResult.getValue() != null) {
                    result.addAll(searchResult.getValue().getSituations());
                }
                break;
            case SELECTED:
                if (selected.getValue() != null) {
                    result.add(selected.getValue());
                }
                break;
            case ALL:
            default:
                addAll(result, getIncidents().getValue());
                addAll(result, getRoadworks().getValue());
                break;
        }
        return result;
    }

    private void addAll(List<Situation> target, List<Situation> source) {
        if (source != null) {
            target.addAll(source);
        }
    }

    /** "Show on map" from the details panel. */
    public void showOnMap(Situation situation) {
        selected.setValue(situation);
        mapMode.setValue(MapMode.SELECTED);
        tabRequest.setValue(TAB_MAP);
    }

    /** "Show results on map" from the search screen. */
    public void showSearchResultsOnMap() {
        mapMode.setValue(MapMode.SEARCH);
        tabRequest.setValue(TAB_MAP);
    }

    // ---------- navigation ----------

    public LiveData<String> getTabRequest() {
        return tabRequest;
    }

    public void clearTabRequest() {
        tabRequest.setValue(null);
    }

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

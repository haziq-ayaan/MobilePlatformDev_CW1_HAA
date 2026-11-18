// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahmad_haziq_s2337383.R;
import com.example.ahmad_haziq_s2337383.viewmodel.SearchResult;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Locale;

/**
 * Search screen with two modes:
 *  - by road: type a road number (with suggestions from the current data) or a place name;
 *  - by distance: everything within N km of your current location or of a fixed place.
 */
public class SearchFragment extends Fragment {

    // Fixed starting points. Index 0 means "use the phone's location".
    private static final String[] PLACE_NAMES = {
            "My current location", "GCU, Glasgow", "Edinburgh", "Stirling",
            "Perth", "Dundee", "Aberdeen", "Inverness", "Dumfries"};
    private static final double[][] PLACE_COORDS = {
            {0, 0}, {55.8667, -4.2501}, {55.9533, -3.1883}, {56.1165, -3.9369},
            {56.3950, -3.4308}, {56.4620, -2.9707}, {57.1497, -2.0943}, {57.4778, -4.2247}, {55.0700, -3.6050}};

    private TrafficViewModel viewModel;
    private MaterialButtonToggleGroup modeToggle;
    private View roadForm;
    private View distanceForm;
    private MaterialAutoCompleteTextView roadInput;
    private MaterialAutoCompleteTextView originInput;
    private Slider radiusSlider;
    private TextView radiusLabel;
    private Chip incidentsChip;
    private Chip roadworksChip;
    private TextView resultTitle;
    private MaterialButton showOnMapButton;
    private SituationAdapter adapter;
    private int selectedPlace = 1; // GCU by default

    // Asks for location permission; if refused we fall back to a fixed place
    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    searchFromMyLocation();
                } else {
                    Toast.makeText(requireContext(), R.string.location_denied, Toast.LENGTH_LONG).show();
                    selectPlace(1);
                    runDistanceSearch(PLACE_NAMES[1], PLACE_COORDS[1][0], PLACE_COORDS[1][1]);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        modeToggle = view.findViewById(R.id.modeToggle);
        roadForm = view.findViewById(R.id.roadForm);
        distanceForm = view.findViewById(R.id.distanceForm);
        roadInput = view.findViewById(R.id.roadInput);
        originInput = view.findViewById(R.id.originInput);
        radiusSlider = view.findViewById(R.id.radiusSlider);
        radiusLabel = view.findViewById(R.id.radiusLabel);
        incidentsChip = view.findViewById(R.id.incidentsChip);
        roadworksChip = view.findViewById(R.id.roadworksChip);
        resultTitle = view.findViewById(R.id.resultTitle);
        showOnMapButton = view.findViewById(R.id.showOnMapButton);

        // switch between the two forms
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateForm(checkedId);
            }
        });
        updateForm(modeToggle.getCheckedButtonId());

        // road suggestions come from whatever is in the data right now
        viewModel.getIncidents().observe(getViewLifecycleOwner(), list -> updateRoadSuggestions());
        viewModel.getRoadworks().observe(getViewLifecycleOwner(), list -> updateRoadSuggestions());
        roadInput.setOnItemClickListener((parent, v, position, id) -> runRoadSearch());
        roadInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                runRoadSearch();
                return true;
            }
            return false;
        });

        // distance form
        originInput.setSimpleItems(PLACE_NAMES);
        if (savedInstanceState == null) {
            selectPlace(1);
        }
        originInput.setOnItemClickListener((parent, v, position, id) -> selectedPlace = position);
        radiusSlider.addOnChangeListener((slider, value, fromUser) -> updateRadiusLabel());
        updateRadiusLabel();

        view.findViewById(R.id.searchButton).setOnClickListener(v -> {
            if (modeToggle.getCheckedButtonId() == R.id.modeRoad) {
                runRoadSearch();
            } else {
                startDistanceSearch();
            }
        });

        // results
        RecyclerView results = view.findViewById(R.id.resultsList);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        results.setNestedScrollingEnabled(false);
        adapter = new SituationAdapter(situation -> viewModel.selectSituation(situation));
        results.setAdapter(adapter);
        showOnMapButton.setOnClickListener(v -> viewModel.showSearchResultsOnMap());

        viewModel.getSearchResult().observe(getViewLifecycleOwner(), this::showResult);
        viewModel.getSelected().observe(getViewLifecycleOwner(),
                s -> adapter.setSelectedId(s == null ? null : s.getSituationId()));
    }

    private void updateForm(int checkedId) {
        boolean byRoad = checkedId != R.id.modeDistance;
        roadForm.setVisibility(byRoad ? View.VISIBLE : View.GONE);
        distanceForm.setVisibility(byRoad ? View.GONE : View.VISIBLE);
    }

    private void updateRoadSuggestions() {
        ArrayAdapter<String> suggestions = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, viewModel.getKnownRoads());
        roadInput.setAdapter(suggestions);
    }

    private void updateRadiusLabel() {
        radiusLabel.setText(getString(R.string.search_radius, Math.round(radiusSlider.getValue())));
    }

    private void selectPlace(int index) {
        selectedPlace = index;
        originInput.setText(PLACE_NAMES[index], false);
    }

    // ---------- road search ----------

    private void runRoadSearch() {
        String road = roadInput.getText() == null ? "" : roadInput.getText().toString().trim();
        if (road.isEmpty()) {
            roadInput.setError(getString(R.string.search_road_empty));
            return;
        }
        if (!checkTypesSelected()) {
            return;
        }
        hideKeyboard();
        roadInput.dismissDropDown();
        viewModel.searchByRoad(road, incidentsChip.isChecked(), roadworksChip.isChecked());
    }

    // ---------- distance search ----------

    private void startDistanceSearch() {
        if (!checkTypesSelected()) {
            return;
        }
        // work out the place from the text, as it may have been restored after rotating
        String chosen = originInput.getText() == null ? "" : originInput.getText().toString();
        for (int i = 0; i < PLACE_NAMES.length; i++) {
            if (PLACE_NAMES[i].equals(chosen)) {
                selectedPlace = i;
            }
        }
        if (selectedPlace > 0) {
            runDistanceSearch(PLACE_NAMES[selectedPlace], PLACE_COORDS[selectedPlace][0], PLACE_COORDS[selectedPlace][1]);
        } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            searchFromMyLocation();
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private void searchFromMyLocation() {
        resultTitle.setVisibility(View.VISIBLE);
        resultTitle.setText(R.string.search_finding_location);
        LocationHelper.getCoarseLocation(requireContext(), location -> {
            if (!isAdded()) {
                return; // user has left the screen meanwhile
            }
            if (location == null) {
                Toast.makeText(requireContext(), R.string.location_unavailable, Toast.LENGTH_LONG).show();
                selectPlace(1);
                runDistanceSearch(PLACE_NAMES[1], PLACE_COORDS[1][0], PLACE_COORDS[1][1]);
            } else {
                String name = String.format(Locale.UK, "you (%.3f, %.3f)", location.getLatitude(), location.getLongitude());
                runDistanceSearch(name, location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void runDistanceSearch(String originName, double lat, double lon) {
        viewModel.searchByDistance(originName, lat, lon, radiusSlider.getValue(),
                incidentsChip.isChecked(), roadworksChip.isChecked());
    }

    // ---------- results ----------

    private void showResult(SearchResult result) {
        if (result == null) {
            resultTitle.setVisibility(View.GONE);
            showOnMapButton.setVisibility(View.GONE);
            adapter.setItems(null);
            return;
        }
        resultTitle.setVisibility(View.VISIBLE);
        resultTitle.setText(result.getDescription());
        showOnMapButton.setVisibility(result.getSituations().isEmpty() ? View.GONE : View.VISIBLE);
        if (result.isDistanceSearch()) {
            adapter.setDistanceOrigin(result.getOriginLat(), result.getOriginLon());
        } else {
            adapter.clearDistanceOrigin();
        }
        adapter.setItems(result.getSituations());
    }

    private boolean checkTypesSelected() {
        if (!incidentsChip.isChecked() && !roadworksChip.isChecked()) {
            Toast.makeText(requireContext(), R.string.search_pick_type, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }
}

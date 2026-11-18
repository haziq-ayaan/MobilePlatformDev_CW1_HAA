// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ahmad_haziq_s2337383.R;
import com.example.ahmad_haziq_s2337383.model.LocationPoint;
import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.model.SituationLocation;
import com.example.ahmad_haziq_s2337383.model.SituationType;
import com.example.ahmad_haziq_s2337383.viewmodel.SearchResult;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel.MapMode;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Google Map showing situations as icons, with a coloured line along the affected stretch of road
 * (each situation has a "from" and a "to" point). The chips at the top choose what is shown:
 * everything, just incidents, just roadworks, the latest search results, or the selected item.
 */
public class TrafficMapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private TrafficViewModel viewModel;
    private GoogleMap map;
    private ChipGroup modeChips;
    private Chip searchChip;
    private Chip selectedChip;
    private TextView mapInfo;
    private final Map<SituationType, BitmapDescriptor> iconCache = new EnumMap<>(SituationType.class);
    private boolean updatingChips = false;
    private int lastDrawn = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        modeChips = view.findViewById(R.id.mapModeChips);
        searchChip = view.findViewById(R.id.chipSearch);
        selectedChip = view.findViewById(R.id.chipSelected);
        mapInfo = view.findViewById(R.id.mapInfo);

        modeChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updatingChips || checkedIds.isEmpty()) {
                return;
            }
            viewModel.setMapMode(modeForChip(checkedIds.get(0)));
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // redraw whenever anything the map depends on changes
        // a data refresh only moves the camera if nothing was drawn before,
        // so the hourly update does not throw away where the user has panned to
        viewModel.getIncidents().observe(getViewLifecycleOwner(), l -> redraw(lastDrawn == 0));
        viewModel.getRoadworks().observe(getViewLifecycleOwner(), l -> redraw(lastDrawn == 0));
        viewModel.getSearchResult().observe(getViewLifecycleOwner(), r -> redraw(true));
        viewModel.getSelected().observe(getViewLifecycleOwner(), s -> redraw(true));
        viewModel.getMapMode().observe(getViewLifecycleOwner(), mode -> {
            checkChipFor(mode);
            redraw(true);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6));
        // tapping a marker's label opens that situation's details
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) {
                viewModel.selectSituation((Situation) tag);
            }
        });
        redraw(true);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            redraw(true); // tab has just been opened, so make sure the view fits what is shown
        }
    }

    private void redraw(boolean fitCamera) {
        if (map == null || !isAdded()) {
            return;
        }
        searchChip.setEnabled(viewModel.getSearchResult().getValue() != null);
        selectedChip.setEnabled(viewModel.getSelected().getValue() != null);

        map.clear();
        List<Situation> situations = viewModel.getMapSituations();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int drawn = 0;
        for (Situation s : situations) {
            if (addToMap(s, bounds)) {
                drawn++;
            }
        }

        // for a distance search, also draw the search area
        MapMode mode = viewModel.getMapMode().getValue();
        SearchResult search = viewModel.getSearchResult().getValue();
        if (mode == MapMode.SEARCH && search != null && search.isDistanceSearch()) {
            LatLng origin = new LatLng(search.getOriginLat(), search.getOriginLon());
            map.addCircle(new CircleOptions().center(origin).radius(search.getRadiusKm() * 1000)
                    .strokeColor(ContextCompat.getColor(requireContext(), R.color.map_search_area))
                    .fillColor(ContextCompat.getColor(requireContext(), R.color.map_search_area_fill))
                    .strokeWidth(3));
            map.addMarker(new MarkerOptions().position(origin).title("Search centre")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            bounds.include(origin);
            drawn++;
        }

        mapInfo.setText(describe(mode, situations.size(), search));
        if (fitCamera) {
            moveCamera(bounds, drawn, situations);
        }
        lastDrawn = drawn;
    }

    /** Adds one situation (marker + line) and returns true if it had coordinates. */
    private boolean addToMap(Situation s, LatLngBounds.Builder bounds) {
        SituationLocation location = s.getLocation();
        double[] centre = location.getCentre();
        if (centre == null) {
            return false;
        }
        LatLng position = new LatLng(centre[0], centre[1]);
        Marker marker = map.addMarker(new MarkerOptions()
                .position(position)
                .title(s.getType().getLabel() + " – " + s.getPrimaryRoad())
                .snippet(s.getComment())
                .icon(iconFor(s.getType()))
                .anchor(0.5f, 0.5f));
        if (marker != null) {
            marker.setTag(s);
        }
        bounds.include(position);

        if (location.isLine()) {
            LocationPoint from = location.getFrom();
            LocationPoint to = location.getTo();
            int colour = s.getCategory() == SituationCategory.ROADWORKS
                    ? SituationStyle.colourFor(s.getDurationBand()) : R.color.map_incident_line;
            map.addPolyline(new PolylineOptions()
                    .add(new LatLng(from.getLatitude(), from.getLongitude()),
                            new LatLng(to.getLatitude(), to.getLongitude()))
                    .color(ContextCompat.getColor(requireContext(), colour))
                    .width(10));
            bounds.include(new LatLng(from.getLatitude(), from.getLongitude()));
            bounds.include(new LatLng(to.getLatitude(), to.getLongitude()));
        }
        return true;
    }

    private void moveCamera(LatLngBounds.Builder bounds, int drawn, List<Situation> situations) {
        if (drawn == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6));
            return;
        }
        MapMode mode = viewModel.getMapMode().getValue();
        if (mode == MapMode.SELECTED && situations.size() == 1) {
            double[] centre = situations.get(0).getLocation().getCentre();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(centre[0], centre[1]), 13));
            return;
        }
        try {
            int padding = Math.round(48 * getResources().getDisplayMetrics().density);
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding));
        } catch (IllegalStateException e) {
            // the map has not been laid out yet (e.g. tab hidden) - just centre on Scotland
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6));
        }
    }

    private String describe(MapMode mode, int count, SearchResult search) {
        if (mode == MapMode.SEARCH) {
            return search == null ? getString(R.string.map_no_search) : "Search: " + search.getDescription();
        }
        if (mode == MapMode.SELECTED) {
            Situation s = viewModel.getSelected().getValue();
            return s == null ? getString(R.string.map_nothing_selected)
                    : "Selected: " + s.getType().getLabel() + " – " + s.getPrimaryRoad();
        }
        return "Showing " + count + (count == 1 ? " situation" : " situations") + " · tap a label for details";
    }

    /** Draws one of our vector icons onto a bitmap, because map markers need bitmaps. */
    private BitmapDescriptor iconFor(SituationType type) {
        BitmapDescriptor cached = iconCache.get(type);
        if (cached != null) {
            return cached;
        }
        Drawable drawable = ContextCompat.getDrawable(requireContext(), SituationStyle.iconFor(type));
        int size = Math.round(30 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        if (drawable != null) {
            drawable.setBounds(0, 0, size, size);
            drawable.draw(new Canvas(bitmap));
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        iconCache.put(type, descriptor);
        return descriptor;
    }

    // ---------- chips ----------

    private MapMode modeForChip(int chipId) {
        if (chipId == R.id.chipIncidents) return MapMode.INCIDENTS;
        if (chipId == R.id.chipRoadworks) return MapMode.ROADWORKS;
        if (chipId == R.id.chipSearch) return MapMode.SEARCH;
        if (chipId == R.id.chipSelected) return MapMode.SELECTED;
        return MapMode.ALL;
    }

    private void checkChipFor(MapMode mode) {
        int chipId;
        switch (mode) {
            case INCIDENTS:
                chipId = R.id.chipIncidents;
                break;
            case ROADWORKS:
                chipId = R.id.chipRoadworks;
                break;
            case SEARCH:
                chipId = R.id.chipSearch;
                break;
            case SELECTED:
                chipId = R.id.chipSelected;
                break;
            default:
                chipId = R.id.chipAll;
                break;
        }
        updatingChips = true; // stops the listener feeding the same value back
        modeChips.check(chipId);
        updatingChips = false;
    }
}

// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ahmad_haziq_s2337383.R;
import com.example.ahmad_haziq_s2337383.model.DurationBand;
import com.example.ahmad_haziq_s2337383.model.Impact;
import com.example.ahmad_haziq_s2337383.model.LocationPoint;
import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.model.SituationLocation;
import com.example.ahmad_haziq_s2337383.model.Validity;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.Map;

/**
 * Full details of the selected situation, grouped into sections
 * (summary, when, impact, location, record details).
 * Rows are added in code because which ones exist depends on the data.
 */
public class DetailFragment extends Fragment {

    private TrafficViewModel viewModel;
    private ScrollView scrollView;
    private View emptyView;
    private ImageView headerIcon;
    private TextView headerTitle;
    private TextView headerSubtitle;
    private TextView commentText;
    private LinearLayout sections;
    private MaterialButton showOnMapButton;
    private Situation current;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        scrollView = view.findViewById(R.id.detailScroll);
        emptyView = view.findViewById(R.id.emptyDetail);
        headerIcon = view.findViewById(R.id.headerIcon);
        headerTitle = view.findViewById(R.id.headerTitle);
        headerSubtitle = view.findViewById(R.id.headerSubtitle);
        commentText = view.findViewById(R.id.commentText);
        sections = view.findViewById(R.id.sections);
        showOnMapButton = view.findViewById(R.id.showOnMapButton);
        showOnMapButton.setOnClickListener(v -> {
            if (current != null) {
                viewModel.showOnMap(current);
            }
        });

        // Close button only makes sense in portrait, where the details cover the list
        MaterialButton closeButton = view.findViewById(R.id.closeButton);
        boolean twoPane = getResources().getBoolean(R.bool.two_pane);
        closeButton.setVisibility(twoPane ? View.GONE : View.VISIBLE);
        closeButton.setOnClickListener(v -> viewModel.closeDetail());

        viewModel.getSelected().observe(getViewLifecycleOwner(), this::show);
    }

    private void show(Situation s) {
        current = s;
        if (s == null) {
            scrollView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        scrollView.scrollTo(0, 0);

        headerIcon.setImageResource(SituationStyle.iconFor(s.getType()));
        headerIcon.setContentDescription(s.getType().getLabel());
        headerTitle.setText(s.getType().getLabel());
        String subtitle = s.getRoads().isEmpty() ? s.getCategory().getLabel() : String.join(", ", s.getRoads());
        if (s.getDirectionText() != null) {
            subtitle += " · " + s.getDirectionText();
        }
        headerSubtitle.setText(subtitle);
        commentText.setText(s.getComment() != null ? s.getComment() : "No description given");
        showOnMapButton.setVisibility(s.getLocation().hasCoordinates() ? View.VISIBLE : View.GONE);

        sections.removeAllViews();
        addWhenSection(s);
        addImpactSection(s);
        addLocationSection(s.getLocation());
        addRecordSection(s);
    }

    // ---------- sections ----------

    private void addWhenSection(Situation s) {
        Validity v = s.getValidity();
        addSection("When");
        addRow("Started", Validity.format(v.getStartTime()));
        if (v.getEndTime() != null) {
            addRow("Planned end", Validity.format(v.getEndTime()));
        }
        if (s.getCategory() == SituationCategory.ROADWORKS) {
            DurationBand band = s.getDurationBand();
            addRow("Duration", v.getReadableDuration() + " – " + band.getLabel().toLowerCase(Locale.UK)
                    + " (" + band.getDescription().toLowerCase(Locale.UK) + ")", SituationStyle.colourFor(band));
        }
        addRow("Last updated", Validity.format(s.getVersionTime()));
        addRow("First reported", Validity.format(s.getCreationTime()));
    }

    private void addImpactSection(Situation s) {
        Impact impact = s.getImpact();
        addSection("Impact");
        if (s.getWorksDescription() != null) {
            addRow("Works", s.getWorksDescription());
        }
        addRow("Delays", impact.getDelayLevel().getLabel(), SituationStyle.colourFor(impact.getDelayLevel()));
        if (impact.getOperationalLanes() != null) {
            addRow("Lanes open", impact.getOperationalLanes() == 0
                    ? "None (closed)" : String.valueOf(impact.getOperationalLanes()));
        }
        if (impact.getLanesRestricted() != null) {
            addRow("Lanes restricted", String.valueOf(impact.getLanesRestricted()));
        }
        if (s.getComplianceOption() != null) {
            addRow("Compliance", readable(s.getComplianceOption()));
        }
    }

    private void addLocationSection(SituationLocation location) {
        addSection("Location");
        if (location.getReadableDirection() != null) {
            addRow("Direction", location.getReadableDirection());
        }
        addPoint("From", location.getFrom());
        addPoint("To", location.getTo());
        if (location.getSegmentType() != null) {
            addRow("Location type", readable(location.getSegmentType()));
        }
    }

    private void addPoint(String label, LocationPoint point) {
        if (point == null) {
            return;
        }
        addRow(label, point.getDisplayName());
        if (point.hasCoordinates()) {
            addRow("", String.format(Locale.UK, "%.5f, %.5f", point.getLatitude(), point.getLongitude()));
        }
    }

    private void addRecordSection(Situation s) {
        addSection("Record details");
        addRow("Feed", s.getCategory().getLabel());
        addRow("Record type", readable(s.getRecordType()));
        if (s.getSubType() != null) {
            addRow("Sub-type", readable(s.getSubType()));
        }
        addRow("Probability", readable(s.getProbability()));
        addRow("Source", s.getSourceName() + (s.getSourceType() != null ? " (" + readable(s.getSourceType()) + ")" : ""));
        addRow("Status", readable(s.getInformationStatus()));
        addRow("Reference", s.getSituationId());
        for (Map.Entry<String, String> extra : s.getOtherDetails().entrySet()) {
            addRow(extra.getKey(), readable(extra.getValue()));
        }
    }

    // ---------- helpers ----------

    private void addSection(String title) {
        TextView header = (TextView) getLayoutInflater().inflate(R.layout.item_detail_section, sections, false);
        header.setText(title);
        sections.addView(header);
    }

    private void addRow(String label, String value) {
        addRow(label, value, 0);
    }

    private void addRow(String label, String value, @ColorRes int valueColour) {
        if (value == null || value.isEmpty() || value.equals("null")) {
            return;
        }
        View row = getLayoutInflater().inflate(R.layout.item_detail_row, sections, false);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        TextView valueView = row.findViewById(R.id.rowValue);
        valueView.setText(value);
        if (valueColour != 0) {
            valueView.setTextColor(ContextCompat.getColor(requireContext(), valueColour));
        }
        sections.addView(row);
    }

    /** Turns feed values like "roadAuthorities" into "Road authorities". */
    private String readable(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(value.charAt(i - 1))) {
                sb.append(' ').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return Character.toUpperCase(sb.charAt(0)) + sb.substring(1);
    }
}

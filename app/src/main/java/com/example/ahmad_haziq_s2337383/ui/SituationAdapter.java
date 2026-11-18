// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahmad_haziq_s2337383.R;
import com.example.ahmad_haziq_s2337383.model.DelayLevel;
import com.example.ahmad_haziq_s2337383.model.DurationBand;
import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.model.Validity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows situations as cards: icon, type, road, the generalPublicComment text,
 * and a line with the time/duration. Roadworks get a coloured strip for duration
 * and coloured delay text.
 */
public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClick(Situation situation);
    }

    private final List<Situation> items = new ArrayList<>();
    private final OnSituationClickListener listener;
    private String selectedId;
    // set for distance search results, so each card can show how far away it is
    private boolean showDistance = false;
    private double originLat;
    private double originLon;

    public SituationAdapter(OnSituationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Situation> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setDistanceOrigin(double lat, double lon) {
        showDistance = true;
        originLat = lat;
        originLon = lon;
    }

    public void clearDistanceOrigin() {
        showDistance = false;
    }

    /** Highlights the selected card (useful in landscape where the details are shown alongside). */
    public void setSelectedId(String id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Situation s = items.get(position);
        boolean isSelected = s.getSituationId() != null && s.getSituationId().equals(selectedId);
        String distance = showDistance
                ? String.format(Locale.UK, "%.1f km away", s.distanceKmTo(originLat, originLon)) : null;
        holder.bind(s, isSelected, distance, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final View durationStripe;
        private final ImageView icon;
        private final TextView typeText;
        private final TextView roadText;
        private final TextView commentText;
        private final TextView metaText;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            durationStripe = itemView.findViewById(R.id.durationStripe);
            icon = itemView.findViewById(R.id.icon);
            typeText = itemView.findViewById(R.id.typeText);
            roadText = itemView.findViewById(R.id.roadText);
            commentText = itemView.findViewById(R.id.commentText);
            metaText = itemView.findViewById(R.id.metaText);
        }

        void bind(Situation s, boolean isSelected, String distance, OnSituationClickListener listener) {
            Context context = itemView.getContext();

            icon.setImageResource(SituationStyle.iconFor(s.getType()));
            icon.setContentDescription(s.getType().getLabel());
            typeText.setText(s.getType().getLabel());
            roadText.setText(s.getPrimaryRoad());
            commentText.setText(s.getComment() != null ? s.getComment() : "No description given");

            if (s.getCategory() == SituationCategory.ROADWORKS) {
                DurationBand band = s.getDurationBand();
                int bandColour = ContextCompat.getColor(context, SituationStyle.colourFor(band));
                durationStripe.setVisibility(View.VISIBLE);
                durationStripe.setBackgroundColor(bandColour);

                DelayLevel delay = s.getDelayLevel();
                String meta = band.getLabel() + " (" + s.getValidity().getReadableDuration() + ")"
                        + " · " + delay.getLabel();
                metaText.setText(meta);
                metaText.setTextColor(ContextCompat.getColor(context, SituationStyle.colourFor(delay)));
            } else {
                durationStripe.setVisibility(View.INVISIBLE);
                String meta = "Since " + Validity.format(s.getValidity().getStartTime());
                if (s.getDirectionText() != null) {
                    meta += " · " + s.getDirectionText();
                }
                metaText.setText(meta);
                metaText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }

            if (distance != null) {
                metaText.setText(distance + " · " + metaText.getText());
            }

            card.setStrokeWidth(isSelected ? dp(context, 2) : 0);
            card.setOnClickListener(v -> listener.onSituationClick(s));
        }

        private static int dp(Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}

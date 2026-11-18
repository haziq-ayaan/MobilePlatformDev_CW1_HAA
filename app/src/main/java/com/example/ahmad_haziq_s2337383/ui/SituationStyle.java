// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import com.example.ahmad_haziq_s2337383.R;
import com.example.ahmad_haziq_s2337383.model.DelayLevel;
import com.example.ahmad_haziq_s2337383.model.DurationBand;
import com.example.ahmad_haziq_s2337383.model.SituationType;

/** Keeps the icon and colour choices in one place so the list, detail and map all match. */
public final class SituationStyle {

    private SituationStyle() {
    }

    @DrawableRes
    public static int iconFor(SituationType type) {
        switch (type) {
            case ROAD_CLOSED:
                return R.drawable.ic_type_road_closed;
            case LANE_RESTRICTION:
                return R.drawable.ic_type_lane_restriction;
            case QUEUING_TRAFFIC:
                return R.drawable.ic_type_queuing;
            case ACCIDENT:
                return R.drawable.ic_type_accident;
            case OBSTRUCTION:
                return R.drawable.ic_type_obstruction;
            case ROAD_CONDITIONS:
                return R.drawable.ic_type_conditions;
            case ROADWORKS_CLOSURE:
                return R.drawable.ic_type_roadworks_closure;
            case ROADWORKS_LANE_CLOSURE:
                return R.drawable.ic_type_roadworks_lane;
            case ROADWORKS_TRAFFIC_LIGHTS:
                return R.drawable.ic_type_roadworks_lights;
            case ROADWORKS_URGENT:
                return R.drawable.ic_type_roadworks_urgent;
            case ROADWORKS:
                return R.drawable.ic_type_roadworks;
            case INCIDENT_OTHER:
            default:
                return R.drawable.ic_type_incident_other;
        }
    }

    @ColorRes
    public static int colourFor(DurationBand band) {
        switch (band) {
            case SHORT:
                return R.color.duration_short;
            case MEDIUM:
                return R.color.duration_medium;
            case LONG:
                return R.color.duration_long;
            case UNKNOWN:
            default:
                return R.color.duration_unknown;
        }
    }

    @ColorRes
    public static int colourFor(DelayLevel delay) {
        switch (delay) {
            case DELAYS:
                return R.color.delay_delays;
            case LONG_DELAYS:
                return R.color.delay_long;
            case VERY_LONG_DELAYS:
                return R.color.delay_very_long;
            case NONE:
            default:
                return R.color.delay_none;
        }
    }
}

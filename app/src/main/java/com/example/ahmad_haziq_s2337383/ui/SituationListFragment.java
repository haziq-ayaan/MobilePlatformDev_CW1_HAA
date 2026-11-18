// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ahmad_haziq_s2337383.R;
import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel;

import java.util.List;

/** List of either incidents or roadworks (decided by the argument it is created with). */
public class SituationListFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";

    private SituationCategory category;
    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private TextView countText;
    private TextView emptyText;

    public static SituationListFragment newInstance(SituationCategory category) {
        SituationListFragment fragment = new SituationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category.name());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        category = SituationCategory.valueOf(requireArguments().getString(ARG_CATEGORY));

        // activity-scoped, so every fragment shares the same data and selection
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        countText = view.findViewById(R.id.countText);
        emptyText = view.findViewById(R.id.emptyText);
        view.findViewById(R.id.durationLegend)
                .setVisibility(category == SituationCategory.ROADWORKS ? View.VISIBLE : View.GONE);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SituationAdapter(situation -> viewModel.selectSituation(situation));
        recyclerView.setAdapter(adapter);

        LiveData<List<Situation>> source = category == SituationCategory.INCIDENT
                ? viewModel.getIncidents() : viewModel.getRoadworks();
        source.observe(getViewLifecycleOwner(), this::showList);

        viewModel.getSelected().observe(getViewLifecycleOwner(),
                s -> adapter.setSelectedId(s == null ? null : s.getSituationId()));
    }

    private void showList(List<Situation> list) {
        adapter.setItems(list);
        int count = list == null ? 0 : list.size();
        String noun = category == SituationCategory.INCIDENT
                ? (count == 1 ? "incident" : "incidents")
                : (count == 1 ? "roadworks entry" : "roadworks");
        countText.setText(getString(R.string.list_count, count, noun));
        emptyText.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }
}

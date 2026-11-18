// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.ahmad_haziq_s2337383.data.FeedStatus;
import com.example.ahmad_haziq_s2337383.model.SituationCategory;
import com.example.ahmad_haziq_s2337383.ui.DetailFragment;
import com.example.ahmad_haziq_s2337383.ui.SearchFragment;
import com.example.ahmad_haziq_s2337383.ui.SituationListFragment;
import com.example.ahmad_haziq_s2337383.ui.TrafficMapFragment;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;

/**
 * The app's only Activity. It hosts the fragments and swaps between tabs.
 *
 * Portrait  (layout/activity_main.xml):      toolbar, status line, content, bottom navigation;
 *                                            the details panel slides over the list.
 * Landscape (layout-land/activity_main.xml): navigation rail on the left, list and details
 *                                            side by side, status shown in the toolbar.
 *
 * All state lives in TrafficViewModel, so rotating keeps the same tab, selection and data.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_INCIDENTS = "tab_incidents";
    private static final String TAG_ROADWORKS = "tab_roadworks";
    private static final String TAG_SEARCH = "tab_search";
    private static final String TAG_MAP = "tab_map";
    private static final String[] ALL_TAGS = {TAG_INCIDENTS, TAG_ROADWORKS, TAG_SEARCH, TAG_MAP};

    private TrafficViewModel viewModel;
    private MaterialToolbar toolbar;
    private TextView statusText;     // only in the portrait layout
    private View detailContainer;
    private boolean twoPane;
    private OnBackPressedCallback closeDetailOnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED; // stops the bottom nav adding the same padding again
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        statusText = findViewById(R.id.statusText);
        detailContainer = findViewById(R.id.detail_container);
        twoPane = getResources().getBoolean(R.bool.two_pane);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.getStatus().observe(this, this::showStatus);

        // The details fragment always lives in detail_container (both layouts have one)
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(R.id.detail_container) == null) {
            fm.beginTransaction().replace(R.id.detail_container, new DetailFragment()).commit();
        }

        NavigationBarView nav = findViewById(R.id.nav_view);
        nav.setOnItemSelectedListener(item -> {
            viewModel.closeDetail(); // a new tab starts with the list showing
            showTab(item.getItemId());
            return true;
        });
        int tabId = viewModel.getCurrentTabId() != 0 ? viewModel.getCurrentTabId() : R.id.nav_incidents;
        nav.getMenu().findItem(tabId).setChecked(true);
        showTab(tabId);

        // In portrait, Back closes the details panel before leaving the app
        closeDetailOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.closeDetail();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDetailOnBack);
        viewModel.getDetailOpen().observe(this, this::updateDetailPanel);

        // other screens can ask for the map tab (e.g. "Show on map")
        viewModel.getTabRequest().observe(this, request -> {
            if (TrafficViewModel.TAB_MAP.equals(request)) {
                viewModel.clearTabRequest();
                viewModel.closeDetail(); // needed if the map tab is already the selected one
                nav.setSelectedItemId(R.id.nav_map);
            }
        });
    }

    private void showTab(int tabId) {
        viewModel.setCurrentTabId(tabId);
        String tag = tagFor(tabId);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        // Keep each tab's fragment alive and just show/hide them, so lists keep their scroll position
        for (String otherTag : ALL_TAGS) {
            Fragment other = fm.findFragmentByTag(otherTag);
            if (other != null && !otherTag.equals(tag)) {
                transaction.hide(other);
            }
        }
        Fragment target = fm.findFragmentByTag(tag);
        if (target == null) {
            transaction.add(R.id.main_container, createTabFragment(tabId), tag);
        } else {
            transaction.show(target);
        }
        transaction.commit();
    }

    private String tagFor(int tabId) {
        if (tabId == R.id.nav_roadworks) return TAG_ROADWORKS;
        if (tabId == R.id.nav_search) return TAG_SEARCH;
        if (tabId == R.id.nav_map) return TAG_MAP;
        return TAG_INCIDENTS;
    }

    private Fragment createTabFragment(int tabId) {
        if (tabId == R.id.nav_roadworks) {
            return SituationListFragment.newInstance(SituationCategory.ROADWORKS);
        } else if (tabId == R.id.nav_search) {
            return new SearchFragment();
        } else if (tabId == R.id.nav_map) {
            return new TrafficMapFragment();
        }
        return SituationListFragment.newInstance(SituationCategory.INCIDENT);
    }

    private void updateDetailPanel(Boolean open) {
        boolean isOpen = open != null && open;
        if (twoPane) {
            detailContainer.setVisibility(View.VISIBLE); // always visible next to the list
            closeDetailOnBack.setEnabled(false);
        } else {
            detailContainer.setVisibility(isOpen ? View.VISIBLE : View.GONE);
            closeDetailOnBack.setEnabled(isOpen);
        }
    }

    private void showStatus(FeedStatus status) {
        String text = status.getDisplayText();
        if (statusText != null) {
            statusText.setText(text);
            int background;
            int foreground;
            switch (status.getState()) {
                case LIVE:
                    background = R.color.status_live_bg;
                    foreground = R.color.status_live_text;
                    break;
                case OFFLINE:
                    background = R.color.status_offline_bg;
                    foreground = R.color.status_offline_text;
                    break;
                case ERROR:
                    background = R.color.status_error_bg;
                    foreground = R.color.status_error_text;
                    break;
                default:
                    background = R.color.status_loading_bg;
                    foreground = R.color.status_loading_text;
                    break;
            }
            statusText.setBackgroundColor(ContextCompat.getColor(this, background));
            statusText.setTextColor(ContextCompat.getColor(this, foreground));
        } else {
            // landscape has no room for a separate status line, so it goes under the title
            toolbar.setSubtitle(text);
        }
    }

    // ---------- toolbar menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

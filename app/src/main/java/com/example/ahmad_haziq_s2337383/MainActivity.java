// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ahmad_haziq_s2337383.model.Situation;
import com.example.ahmad_haziq_s2337383.viewmodel.TrafficViewModel;

import java.util.List;

/**
 * TEMPORARY test screen (step 2): shows the download status and a text summary of the parsed data,
 * to check the download / parsing / threading layer works. Replaced by the real fragments in step 3.
 */
public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView outputText;
    private TrafficViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusText);
        outputText = findViewById(R.id.outputText);
        Button refreshButton = findViewById(R.id.refreshButton);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        // Observers run on the UI thread whenever the background thread posts new values
        viewModel.getStatus().observe(this, status -> statusText.setText(status.getDisplayText()));
        viewModel.getIncidents().observe(this, list -> showSummary());
        viewModel.getRoadworks().observe(this, list -> showSummary());

        refreshButton.setOnClickListener(v -> viewModel.refresh());
    }

    private void showSummary() {
        List<Situation> incidents = viewModel.getIncidents().getValue();
        List<Situation> roadworks = viewModel.getRoadworks().getValue();
        StringBuilder sb = new StringBuilder();
        sb.append("Incidents: ").append(incidents == null ? 0 : incidents.size()).append('\n');
        sb.append("Roadworks: ").append(roadworks == null ? 0 : roadworks.size()).append("\n\n");
        appendFirst(sb, incidents, 15);
        appendFirst(sb, roadworks, 10);
        outputText.setText(sb.toString());
    }

    private void appendFirst(StringBuilder sb, List<Situation> list, int max) {
        if (list == null) return;
        for (int i = 0; i < Math.min(max, list.size()); i++) {
            Situation s = list.get(i);
            sb.append("• [").append(s.getType().getLabel()).append("] ")
                    .append(s.getComment()).append("\n\n");
        }
    }
}

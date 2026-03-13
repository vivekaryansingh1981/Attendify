package com.codingbros.attendify;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class StudTimetableActivity extends AppCompatActivity {

    private GridLayout gridTimetable;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    // CHANGED: Array updated to hold 40 cells (5 days * 8 slots)
    private TextView[] gridCells = new TextView[40];
    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final int SLOTS_PER_DAY = 8; // CHANGED

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stud_timetable);

        db = FirebaseFirestore.getInstance();
        gridTimetable = findViewById(R.id.grid_timetable);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        setupReadOnlyGrid();

        fetchAllTimetables();
    }

    private void setupReadOnlyGrid() {
        gridTimetable.removeAllViews();

        for (int i = 0; i < SLOTS_PER_DAY; i++) {
            for (int j = 0; j < DAYS.length; j++) {
                TextView textView = new TextView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(j, 1f);
                params.rowSpec = GridLayout.spec(i, 1f);
                params.setMargins(4, 4, 4, 4);
                textView.setLayoutParams(params);

                textView.setGravity(Gravity.CENTER);
                textView.setPadding(4, 24, 4, 24);
                textView.setBackgroundColor(Color.parseColor("#E0F7FA"));
                textView.setTextColor(Color.parseColor("#006064"));
                textView.setTextSize(11f);
                textView.setText("-");

                int index = (i * DAYS.length) + j;
                gridCells[index] = textView;

                gridTimetable.addView(textView);
            }
        }
    }

    private void fetchAllTimetables() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        final int[] completedQueries = {0};

        for (int d = 0; d < DAYS.length; d++) {
            String day = DAYS[d];
            final int dayIndex = d;

            db.collection("timetable").document(day).get()
                    .addOnSuccessListener(daySnap -> {
                        if (daySnap.exists()) {
                            for (int slotIndex = 0; slotIndex < SLOTS_PER_DAY; slotIndex++) {

                                if (daySnap.contains("slot_" + slotIndex)) {
                                    Map<String, String> slotData = (Map<String, String>) daySnap.get("slot_" + slotIndex);

                                    if (slotData != null) {
                                        String abbr = slotData.get("subject_abbr");
                                        String from = slotData.get("time_from");
                                        String to = slotData.get("time_to");
                                        String isBreak = slotData.get("is_break"); // Detect break

                                        int cellIndex = (slotIndex * DAYS.length) + dayIndex;

                                        if (gridCells[cellIndex] != null) {
                                            // --- NEW: Custom Styling for Breaks vs Normal Classes ---
                                            if ("true".equals(isBreak)) {
                                                String display = "☕ " + (abbr != null ? abbr.toUpperCase() : "BREAK") + "\n" + from + "-" + to;
                                                gridCells[cellIndex].setText(display);
                                                gridCells[cellIndex].setTextColor(Color.parseColor("#D84315")); // Deep Orange
                                                gridCells[cellIndex].setBackgroundColor(Color.parseColor("#FFCCBC")); // Light Orange background
                                            } else {
                                                String display = abbr + "\n" + from + "-" + to;
                                                gridCells[cellIndex].setText(display);
                                                gridCells[cellIndex].setTextColor(Color.BLACK);
                                                gridCells[cellIndex].setBackgroundColor(Color.parseColor("#B2EBF2"));
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        completedQueries[0]++;
                        if (completedQueries[0] == DAYS.length) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        }

                    }).addOnFailureListener(e -> {
                        completedQueries[0]++;
                        if (completedQueries[0] == DAYS.length) {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        }
                    });
        }
    }
}
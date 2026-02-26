package com.codingbros.attendify;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class StudTimetableActivity extends AppCompatActivity {

    private GridLayout gridTimetable;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    // Array to hold the 25 read-only text views (5 days * 5 slots)
    private TextView[] gridCells = new TextView[25];
    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final int SLOTS_PER_DAY = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stud_timetable); // Matches your Grid XML

        db = FirebaseFirestore.getInstance();
        gridTimetable = findViewById(R.id.grid_timetable);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        setupReadOnlyGrid();
        fetchAllTimetables();
    }

    private void setupReadOnlyGrid() {
        gridTimetable.removeAllViews();

        for (int i = 0; i < SLOTS_PER_DAY; i++) { // Rows (Slots)
            for (int j = 0; j < DAYS.length; j++) { // Columns (Days)
                TextView textView = new TextView(this);

                // Distribute evenly across the grid
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(j, 1f);
                params.rowSpec = GridLayout.spec(i, 1f);
                params.setMargins(4, 4, 4, 4);
                textView.setLayoutParams(params);

                // Styling for the read-only student cell
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(4, 24, 4, 24);
                textView.setBackgroundColor(Color.parseColor("#E0F7FA")); // Light Teal Box
                textView.setTextColor(Color.parseColor("#006064")); // Dark Teal Text
                textView.setTextSize(11f);
                textView.setText("-"); // Default empty state

                // Calculate the exact index (0 to 24)
                int index = (i * DAYS.length) + j;
                gridCells[index] = textView;

                gridTimetable.addView(textView);
            }
        }
    }

    private void fetchAllTimetables() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // 1. Fetch all faculty members
        db.collection("faculty").get().addOnSuccessListener(facultySnaps -> {

            if (facultySnaps.isEmpty()) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                return;
            }

            int totalFaculty = facultySnaps.size();
            final int[] completedQueries = {0};

            for (DocumentSnapshot facultyDoc : facultySnaps) {

                // 2. Loop through Mon-Fri for each faculty
                for (int d = 0; d < DAYS.length; d++) {
                    String day = DAYS[d];
                    final int dayIndex = d;

                    facultyDoc.getReference().collection("timetable").document(day).get()
                            .addOnSuccessListener(daySnap -> {

                                if (daySnap.exists()) {
                                    // 3. Check slots 0 to 4
                                    for (int slotIndex = 0; slotIndex < SLOTS_PER_DAY; slotIndex++) {

                                        if (daySnap.contains("slot_" + slotIndex)) {
                                            Map<String, String> slotData = (Map<String, String>) daySnap.get("slot_" + slotIndex);

                                            if (slotData != null) {
                                                String abbr = slotData.get("subject_abbr");
                                                String from = slotData.get("time_from");
                                                String to = slotData.get("time_to");

                                                // Format text nicely for the grid
                                                String display = abbr + "\n" + from + "-" + to;

                                                // Locate the exact cell index for this day and slot
                                                int cellIndex = (slotIndex * DAYS.length) + dayIndex;

                                                // Update the UI
                                                gridCells[cellIndex].setText(display);
                                                gridCells[cellIndex].setTextColor(Color.BLACK);
                                                gridCells[cellIndex].setBackgroundColor(Color.parseColor("#B2EBF2")); // Slightly darker teal when active
                                            }
                                        }
                                    }
                                }
                            });
                }

                completedQueries[0]++;
                if (completedQueries[0] == totalFaculty) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                }
            }

        }).addOnFailureListener(e -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error fetching timetables", Toast.LENGTH_SHORT).show();
        });
    }
}
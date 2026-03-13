package com.codingbros.attendify;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class TodaysClassesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StudTimetableAdapter adapter;
    private List<Map<String, String>> fullClassesList;
    private TextView tvNoClasses;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_classes);

        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btn_back);
        tvNoClasses = findViewById(R.id.tv_no_classes);
        recyclerView = findViewById(R.id.recycler_todays_classes);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fullClassesList = new ArrayList<>();
        adapter = new StudTimetableAdapter(fullClassesList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        fetchAllTodaysClasses();
    }

    private void fetchAllTodaysClasses() {
        String currentDay = getDayString();

        if (currentDay.equals("Sunday") || currentDay.equals("Saturday")) {
            recyclerView.setVisibility(View.GONE);
            tvNoClasses.setVisibility(View.VISIBLE);
            tvNoClasses.setText("No classes on weekends!");
            return;
        }

        db.collectionGroup("timetable").get().addOnSuccessListener(queryDocumentSnapshots -> {
            fullClassesList.clear();

            if (!queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                    if (documentSnapshot.getId().equals(currentDay) && documentSnapshot.getData() != null) {
                        Map<String, Object> data = documentSnapshot.getData();
                        for (Map.Entry<String, Object> entry : data.entrySet()) {
                            if (entry.getValue() instanceof Map) {
                                fullClassesList.add((Map<String, String>) entry.getValue());
                            }
                        }
                    }
                }
            }

            if (fullClassesList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                tvNoClasses.setVisibility(View.VISIBLE);
            } else {
                // Sort by time accurately
                java.util.Collections.sort(fullClassesList, (c1, c2) -> {
                    int time1 = parseTimeToMinutes(c1.get("time_from"));
                    int time2 = parseTimeToMinutes(c2.get("time_from"));
                    return Integer.compare(time1, time2);
                });

                tvNoClasses.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private String getDayString() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "Monday";
        }
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0;
        try {
            timeStr = timeStr.trim().toUpperCase();
            boolean isPM = timeStr.contains("PM");
            boolean isAM = timeStr.contains("AM");

            String cleanTime = timeStr.replace("AM", "").replace("PM", "").trim();
            String[] parts = cleanTime.split(":");

            int hours = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0;
            int minutes = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

            if (isPM && hours != 12) hours += 12;
            if (isAM && hours == 12) hours = 0;

            return (hours * 60) + minutes;
        } catch (Exception e) {
            return 0;
        }
    }
}
package com.codingbros.attendify;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcademicCalendarActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CalendarEventAdapter adapter;
    private List<Map<String, String>> eventList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academic_calendar);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_calendar_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CalendarEventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        loadCalendarData();
    }

    private void loadCalendarData() {
        eventList.clear();

        db.collection("academic_calendar").document("current")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {

                        String acadYear = document.getString("academicYear");
                        String semNumber = document.getString("semesterNumber");
                        String semType = document.getString("semester");

                        String termStart = document.getString("termStart");
                        String ct1 = document.getString("ct1");
                        String ct2 = document.getString("ct2");
                        String termEnd = document.getString("termEnd");
                        String pracExam = document.getString("practicalExam");
                        String semExam = document.getString("semesterExam");
                        String result = document.getString("resultExpected");

                        // --- NEW: Add Term Details as the very first card ---
                        if (acadYear != null && semNumber != null) {
                            String termDetails = "Semester " + semNumber + " (" + semType + ") | Year: " + acadYear;
                            addEvent("Academic Term", termDetails);
                        }

                        // Add remaining events chronologically
                        if (termStart != null && !termStart.isEmpty()) addEvent("Term Start", termStart);
                        if (ct1 != null && !ct1.isEmpty()) addEvent("Class Test 1", ct1);
                        if (ct2 != null && !ct2.isEmpty()) addEvent("Class Test 2", ct2);
                        if (termEnd != null && !termEnd.isEmpty()) addEvent("Term End", termEnd);
                        if (pracExam != null && !pracExam.isEmpty()) addEvent("Practical Exam", pracExam);
                        if (semExam != null && !semExam.isEmpty()) addEvent("Semester Exam", semExam);
                        if (result != null && !result.isEmpty()) addEvent("Result Expected", result);

                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Academic Calendar not published yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching calendar.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addEvent(String title, String date) {
        Map<String, String> event = new HashMap<>();
        event.put("title", title);
        event.put("date", date);
        eventList.add(event);
    }
}
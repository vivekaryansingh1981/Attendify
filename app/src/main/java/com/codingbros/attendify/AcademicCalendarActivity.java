package com.codingbros.attendify;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AcademicCalendarActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CalendarEventAdapter adapter;
    private List<Map<String, String>> eventList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academic_calendar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_calendar_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CalendarEventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        loadCalendarData();
    }

    private void loadCalendarData() {
        // Clear previous data
        eventList.clear();

        // Hardcoded data as requested (Will be replaced with Firebase fetch later)
        addEvent("Term Start", "15 Dec 2025");
        addEvent("Class Test 1", "27 Jan - 29 Jan, 2026");
        addEvent("Class Test 2", "30 Mar - 02 Apr, 2026");
        addEvent("Term End", "04 Apr 2026");
        addEvent("Practical Exam", "08 Apr 2026 - 18 Apr 2026");
        addEvent("Semester Exam", "23 Apr 2026 - 16 May 2026");
        addEvent("Result Expected", "3rd week of June tentatively");

        adapter.notifyDataSetChanged();
    }

    private void addEvent(String title, String date) {
        Map<String, String> event = new HashMap<>();
        event.put("title", title);
        event.put("date", date);
        eventList.add(event);
    }
}
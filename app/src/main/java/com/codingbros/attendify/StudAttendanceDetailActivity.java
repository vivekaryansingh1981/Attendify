package com.codingbros.attendify;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudAttendanceDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvTotal, tvPresent, tvAbsent, tvMonthYear;
    private ImageView btnPrevMonth, btnNextMonth;
    private RecyclerView recyclerCalendar;
    private FirebaseFirestore db;
    private String subjectName, studUid;

    // Track the currently displayed month
    private Calendar displayCalendar = Calendar.getInstance();

    // Cache ALL records to avoid re-fetching when changing months
    // Key = docId (e.g. 24-02-2026_Maths), Value = Status
    private Map<String, String> allAttendanceRecords = new HashMap<>();

    // Map for the specific month being viewed
    private Map<String, String> currentMonthStatusMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studattendance_detail);

        // --- FIX FOR THE WHITE STATUS BAR GAP ---
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#4CB5C3"));

        subjectName = getIntent().getStringExtra("subject_name");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tv_subject_title);
        tvTotal = findViewById(R.id.tv_total_lectures);
        tvPresent = findViewById(R.id.tv_present_count);
        tvAbsent = findViewById(R.id.tv_absent_count);
        tvMonthYear = findViewById(R.id.tv_month_year);
        recyclerCalendar = findViewById(R.id.recycler_calendar);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        if (subjectName != null) {
            tvTitle.setText(subjectName);
            fetchAttendanceRecords();
        }

        // Setup Next and Previous Month click listeners
        btnPrevMonth.setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });

        btnNextMonth.setOnClickListener(v -> {
            displayCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });
    }

    private void fetchAttendanceRecords() {
        db.collection("attendance")
                .whereEqualTo("subject", subjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    allAttendanceRecords.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId(); // e.g., "24-02-2026_Mathematics"

                        if (document.contains("attendanceData")) {
                            Map<String, String> attendanceData = (Map<String, String>) document.get("attendanceData");

                            if (attendanceData != null && attendanceData.containsKey(studUid)) {
                                String status = attendanceData.get(studUid);
                                // Save all records to cache
                                allAttendanceRecords.put(docId, status);
                            }
                        }
                    }

                    // Render the initial calendar for the current month
                    updateCalendarView();

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching records", Toast.LENGTH_SHORT).show());
    }

    private void updateCalendarView() {
        int total = 0;
        int present = 0;
        int absent = 0;

        currentMonthStatusMap.clear();

        // Format target month/year based on what the user is currently viewing
        String targetMonth = String.format(Locale.getDefault(), "%02d", displayCalendar.get(Calendar.MONTH) + 1);
        String targetYear = String.valueOf(displayCalendar.get(Calendar.YEAR));

        // Update the Month Title TextView
        SimpleDateFormat monthDate = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthDate.format(displayCalendar.getTime()));

        // Filter the cached records to calculate stats for the viewed month
        for (Map.Entry<String, String> entry : allAttendanceRecords.entrySet()) {
            String docId = entry.getKey();
            String status = entry.getValue();

            try {
                String datePart = docId.split("_")[0];
                String[] dateComponents = datePart.split("-");
                String dayStr = dateComponents[0];
                String monthStr = dateComponents[1];
                String yearStr = dateComponents[2];

                if (dayStr.startsWith("0")) {
                    dayStr = dayStr.substring(1);
                }

                // If the record matches the month the user is looking at
                if (monthStr.equals(targetMonth) && yearStr.equals(targetYear)) {
                    total++;
                    if ("Present".equalsIgnoreCase(status)) {
                        present++;
                    } else if ("Absent".equalsIgnoreCase(status)) {
                        absent++;
                    }
                    currentMonthStatusMap.put(dayStr, status);
                }
            } catch (Exception e) {
                continue;
            }
        }

        // Update UI Stats for the viewed month
        tvTotal.setText(String.valueOf(total));
        tvPresent.setText(String.valueOf(present));
        tvAbsent.setText(String.valueOf(absent));

        // Generate the exact days for the RecyclerView grid
        List<String> days = new ArrayList<>();
        Calendar cal = (Calendar) displayCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        // Blank spaces before the 1st
        for (int i = 0; i < startDayOfWeek; i++) {
            days.add("");
        }

        for (int i = 1; i <= maxDays; i++) {
            days.add(String.valueOf(i));
        }

        // Apply to Adapter
        CalendarAdapter adapter = new CalendarAdapter(days, currentMonthStatusMap);
        recyclerCalendar.setAdapter(adapter);
    }
}
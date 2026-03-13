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

    private Calendar displayCalendar = Calendar.getInstance();

    private Map<String, String> allAttendanceRecords = new HashMap<>();
    private Map<String, String> currentMonthStatusMap = new HashMap<>();
    private Map<String, String> globalHolidaysMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studattendance_detail);

        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#4CB5C3"));

        // --- FIXED: Get both name and abbreviation ---
        subjectName = getIntent().getStringExtra("subject_name");
        String subjectAbbr = getIntent().getStringExtra("subject_abbr");

        if (subjectAbbr == null || subjectAbbr.trim().isEmpty()) {
            subjectAbbr = subjectName;
        }

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
            tvTitle.setText(subjectAbbr); // --- CHANGED to Abbreviation ---
            fetchAttendanceRecords();
        }

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
                        String docId = document.getId();

                        if (document.contains("attendanceData")) {
                            Map<String, String> attendanceData = (Map<String, String>) document.get("attendanceData");

                            if (attendanceData != null && attendanceData.containsKey(studUid)) {
                                String status = attendanceData.get(studUid);
                                allAttendanceRecords.put(docId, status);
                            }
                        }
                    }

                    db.collection("holidays").get().addOnSuccessListener(holidaySnaps -> {
                        globalHolidaysMap.clear();
                        for (QueryDocumentSnapshot hDoc : holidaySnaps) {
                            globalHolidaysMap.put(hDoc.getId(), "Holiday");
                        }
                        updateCalendarView();
                    }).addOnFailureListener(e -> updateCalendarView());

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching records", Toast.LENGTH_SHORT).show());
    }

    private void updateCalendarView() {
        int total = 0;
        int present = 0;
        int absent = 0;

        currentMonthStatusMap.clear();

        String targetMonth = String.format(Locale.getDefault(), "%02d", displayCalendar.get(Calendar.MONTH) + 1);
        String targetYear = String.valueOf(displayCalendar.get(Calendar.YEAR));

        SimpleDateFormat monthDate = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthDate.format(displayCalendar.getTime()));

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

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        for (Map.Entry<String, String> entry : globalHolidaysMap.entrySet()) {
            try {
                Calendar recordCal = Calendar.getInstance();
                recordCal.setTime(sdf.parse(entry.getKey()));

                if (recordCal.get(Calendar.MONTH) == displayCalendar.get(Calendar.MONTH) &&
                        recordCal.get(Calendar.YEAR) == displayCalendar.get(Calendar.YEAR)) {

                    String day = String.valueOf(recordCal.get(Calendar.DAY_OF_MONTH));
                    currentMonthStatusMap.put(day, "Holiday");
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        tvTotal.setText(String.valueOf(total));
        tvPresent.setText(String.valueOf(present));
        tvAbsent.setText(String.valueOf(absent));

        List<String> days = new ArrayList<>();
        Calendar cal = (Calendar) displayCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 0; i < startDayOfWeek; i++) {
            days.add("");
        }

        for (int i = 1; i <= maxDays; i++) {
            days.add(String.valueOf(i));
        }

        CalendarAdapter adapter = new CalendarAdapter(days, currentMonthStatusMap);
        recyclerCalendar.setAdapter(adapter);
    }
}
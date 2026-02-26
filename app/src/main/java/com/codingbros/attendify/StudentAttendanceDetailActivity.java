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

public class StudentAttendanceDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvTotal, tvPresent, tvAbsent, tvMonthYear;
    private ImageView btnPrevMonth, btnNextMonth, btnBack;
    private RecyclerView recyclerCalendar;
    private FirebaseFirestore db;
    private String subjectName, studentUid;

    private Map<String, String> overallAttendanceMap = new HashMap<>();
    private Map<String, String> currentMonthDisplayMap = new HashMap<>();
    private Calendar currentDisplayMonth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance_detail);

        subjectName = getIntent().getStringExtra("subject_name");
        db = FirebaseFirestore.getInstance();
        studentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        currentDisplayMonth = Calendar.getInstance();

        // Bind UI Elements
        tvTitle = findViewById(R.id.tv_subject_title);
        tvTotal = findViewById(R.id.tv_total_lectures);
        tvPresent = findViewById(R.id.tv_present_count);
        tvAbsent = findViewById(R.id.tv_absent_count);
        tvMonthYear = findViewById(R.id.tv_month_year);
        recyclerCalendar = findViewById(R.id.recycler_calendar);

        btnBack = findViewById(R.id.btn_back);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        if (subjectName != null) {
            tvTitle.setText(subjectName);
        }

        // Setup Calendar Grid
        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        // --- BUTTON CLICK LISTENERS ---
        btnBack.setOnClickListener(v -> finish());

        btnPrevMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            updateCalendarForMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            updateCalendarForMonth();
        });

        // Fetch data from Firebase
        fetchAttendanceData();
    }

    private void fetchAttendanceData() {
        db.collection("attendance")
                .whereEqualTo("subject", subjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    overallAttendanceMap.clear();
                    int total = 0;
                    int present = 0;
                    int absent = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();

                        if (data != null && data.containsKey("attendanceData")) {
                            Map<String, String> studentsMap = (Map<String, String>) data.get("attendanceData");

                            if (studentsMap != null && studentsMap.containsKey(studentUid)) {
                                String status = studentsMap.get(studentUid);
                                String dateStr = doc.getString("date");

                                if (dateStr != null) {
                                    overallAttendanceMap.put(dateStr, status);

                                    total++;
                                    if ("Present".equals(status)) {
                                        present++;
                                    } else if ("Absent".equals(status)) {
                                        absent++;
                                    }
                                }
                            }
                        }
                    }

                    tvTotal.setText(String.valueOf(total));
                    tvPresent.setText(String.valueOf(present));
                    tvAbsent.setText(String.valueOf(absent));

                    updateCalendarForMonth();

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching records", Toast.LENGTH_SHORT).show());
    }

    private void updateCalendarForMonth() {
        currentMonthDisplayMap.clear();

        // IMPORTANT: Make sure your Firebase dates are exactly in "dd-MM-yyyy" format (e.g., "05-03-2026")
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        for (Map.Entry<String, String> entry : overallAttendanceMap.entrySet()) {
            try {
                Calendar recordCal = Calendar.getInstance();
                recordCal.setTime(sdf.parse(entry.getKey()));

                if (recordCal.get(Calendar.MONTH) == currentDisplayMonth.get(Calendar.MONTH) &&
                        recordCal.get(Calendar.YEAR) == currentDisplayMonth.get(Calendar.YEAR)) {

                    String day = String.valueOf(recordCal.get(Calendar.DAY_OF_MONTH));
                    currentMonthDisplayMap.put(day, entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setupCalendarAdapter();
    }

    private void setupCalendarAdapter() {
        List<String> days = new ArrayList<>();

        SimpleDateFormat monthDate = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthDate.format(currentDisplayMonth.getTime()));

        Calendar cal = (Calendar) currentDisplayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 0; i < startDayOfWeek; i++) {
            days.add("");
        }

        for (int i = 1; i <= maxDays; i++) {
            days.add(String.valueOf(i));
        }

        // We pass a NEW HashMap clone to force the adapter to realize the data has changed
        CalendarAdapter adapter = new CalendarAdapter(days, new HashMap<>(currentMonthDisplayMap));
        recyclerCalendar.setAdapter(adapter);
    }
}
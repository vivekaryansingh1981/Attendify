package com.codingbros.attendify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class FacultyOtherFeaturesActivity extends AppCompatActivity {

    private CardView btnAvailability, btnHoliday, btnSendNotices, btnEditCalendar, btnPresentStudents;
    private ImageView btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_other_features);

        // Link Views
        btnBack = findViewById(R.id.btn_back);
        btnAvailability = findViewById(R.id.btn_availability);
        btnHoliday = findViewById(R.id.btn_holiday);
        btnSendNotices = findViewById(R.id.btn_send_notices);
        btnEditCalendar = findViewById(R.id.btn_edit_calendar);
        btnPresentStudents = findViewById(R.id.btn_present_students); // NEW Button

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        // 1. Availability Click
        btnAvailability.setOnClickListener(v -> {
            startActivity(new Intent(FacultyOtherFeaturesActivity.this, FacultyAvailabilityActivity.class));
        });

        // 2. Holiday Declaration Click
        btnHoliday.setOnClickListener(v -> {
            startActivity(new Intent(FacultyOtherFeaturesActivity.this, FacultyDeclareHolidayActivity.class));
        });

        // 3. Send Notices Click
        btnSendNotices.setOnClickListener(v -> {
            startActivity(new Intent(FacultyOtherFeaturesActivity.this, FacultySendNoticeActivity.class));
        });

        // 4. Edit Calendar Click
        btnEditCalendar.setOnClickListener(v -> {
            startActivity(new Intent(FacultyOtherFeaturesActivity.this, FacultyCalendarEditorActivity.class));
        });

        // 5. Present Students Click (NEW)
        btnPresentStudents.setOnClickListener(v -> {
            startActivity(new Intent(FacultyOtherFeaturesActivity.this, FacultyPresentSubjectsActivity.class));
        });
    }
}
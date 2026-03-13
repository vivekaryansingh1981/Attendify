package com.codingbros.attendify;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FacultyCalendarEditorActivity extends AppCompatActivity {

    private TextInputEditText etAcademicYear, etSemesterNumber, etTermStart, etCt1, etCt2, etPracticalExam, etSemesterExam, etTermEnd, etResultExpected;
    private Spinner spinnerSemester;
    private Button btnPublishCalendar;
    private ImageView btnBack;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_calendar_editor);

        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btn_back);
        etAcademicYear = findViewById(R.id.et_academic_year);
        etSemesterNumber = findViewById(R.id.et_semester_number);
        spinnerSemester = findViewById(R.id.spinner_semester);

        etTermStart = findViewById(R.id.et_term_start);
        etCt1 = findViewById(R.id.et_ct1);
        etCt2 = findViewById(R.id.et_ct2);
        etPracticalExam = findViewById(R.id.et_practical_exam);
        etSemesterExam = findViewById(R.id.et_semester_exam);
        etTermEnd = findViewById(R.id.et_term_end);
        etResultExpected = findViewById(R.id.et_result_expected);

        btnPublishCalendar = findViewById(R.id.btn_publish_calendar);

        btnBack.setOnClickListener(v -> finish());

        String[] semesters = {"Odd Semester", "Even Semester"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(adapter);

        // --- NEW: Attach DatePicker logic to the un-typable fields ---
        // Single Dates:
        etTermStart.setOnClickListener(v -> showDatePicker(etTermStart, false));
        etTermEnd.setOnClickListener(v -> showDatePicker(etTermEnd, false));

        // Date Ranges:
        etCt1.setOnClickListener(v -> showDatePicker(etCt1, true));
        etCt2.setOnClickListener(v -> showDatePicker(etCt2, true));
        etPracticalExam.setOnClickListener(v -> showDatePicker(etPracticalExam, true));
        etSemesterExam.setOnClickListener(v -> showDatePicker(etSemesterExam, true));

        loadExistingCalendar();

        btnPublishCalendar.setOnClickListener(v -> gatherCalendarData());
    }

    // --- NEW: Custom DatePicker Handler ---
    private void showDatePicker(TextInputEditText editText, boolean isRange) {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog startDatePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, dayOfMonth);
            String startStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(startCal.getTime());

            if (isRange) {
                // If it's a range, prompt them to pick the End Date immediately after
                Toast.makeText(this, "Now select the End Date", Toast.LENGTH_SHORT).show();

                DatePickerDialog endDatePicker = new DatePickerDialog(this, (view2, year2, month2, dayOfMonth2) -> {
                    Calendar endCal = Calendar.getInstance();
                    endCal.set(year2, month2, dayOfMonth2);
                    String endStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(endCal.getTime());

                    // Combine them
                    editText.setText(startStr + " - " + endStr);
                }, year, month, dayOfMonth);

                // Prevent picking an end date before the start date
                endDatePicker.getDatePicker().setMinDate(startCal.getTimeInMillis());
                endDatePicker.show();

            } else {
                // Single date mode
                editText.setText(startStr);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        startDatePicker.show();
    }

    private void loadExistingCalendar() {
        db.collection("academic_calendar").document("current").get().addOnSuccessListener(document -> {
            if (document.exists()) {
                etAcademicYear.setText(document.getString("academicYear"));
                etSemesterNumber.setText(document.getString("semesterNumber"));
                etTermStart.setText(document.getString("termStart"));
                etCt1.setText(document.getString("ct1"));
                etCt2.setText(document.getString("ct2"));
                etPracticalExam.setText(document.getString("practicalExam"));
                etSemesterExam.setText(document.getString("semesterExam"));
                etTermEnd.setText(document.getString("termEnd"));
                etResultExpected.setText(document.getString("resultExpected"));

                String semester = document.getString("semester");
                if ("Even Semester".equals(semester)) {
                    spinnerSemester.setSelection(1);
                } else {
                    spinnerSemester.setSelection(0);
                }
            }
        });
    }

    private void gatherCalendarData() {
        String academicYear = etAcademicYear.getText().toString().trim();
        String semesterNumber = etSemesterNumber.getText().toString().trim();
        String semester = spinnerSemester.getSelectedItem().toString();

        String termStart = etTermStart.getText().toString().trim();
        String ct1 = etCt1.getText().toString().trim();
        String ct2 = etCt2.getText().toString().trim();
        String pracExam = etPracticalExam.getText().toString().trim();
        String semExam = etSemesterExam.getText().toString().trim();
        String termEnd = etTermEnd.getText().toString().trim();
        String result = etResultExpected.getText().toString().trim();

        if (academicYear.isEmpty() || semesterNumber.isEmpty() || termStart.isEmpty()) {
            Toast.makeText(this, "Academic Year, Semester Number, and Term Start are mandatory!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublishCalendar.setEnabled(false);
        btnPublishCalendar.setText("Publishing...");

        Map<String, Object> calendarData = new HashMap<>();
        calendarData.put("academicYear", academicYear);
        calendarData.put("semesterNumber", semesterNumber);
        calendarData.put("semester", semester);
        calendarData.put("termStart", termStart);
        calendarData.put("ct1", ct1);
        calendarData.put("ct2", ct2);
        calendarData.put("practicalExam", pracExam);
        calendarData.put("semesterExam", semExam);
        calendarData.put("termEnd", termEnd);
        calendarData.put("resultExpected", result);
        calendarData.put("timestamp", System.currentTimeMillis());

        db.collection("academic_calendar").document("current")
                .set(calendarData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Academic Calendar Published!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublishCalendar.setEnabled(true);
                    btnPublishCalendar.setText("Publish Academic Calendar");
                    Toast.makeText(this, "Failed to publish. Try again.", Toast.LENGTH_SHORT).show();
                });
    }
}
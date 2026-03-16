package com.codingbros.attendify;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FacultyDeclareHolidayActivity extends AppCompatActivity {

    private Button btnStartDate, btnEndDate, btnConfirmHoliday, btnCancelHoliday;
    private TextView tvDateRange;
    private TextInputEditText etHolidayReason;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private String facultyUid;
    private String facultyName = "Faculty";
    private Calendar startCal, endCal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_declare_holiday);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        fetchFacultyName();

        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        btnConfirmHoliday = findViewById(R.id.btn_confirm_holiday);
        btnCancelHoliday = findViewById(R.id.btn_cancel_holiday);
        tvDateRange = findViewById(R.id.tv_date_range);
        etHolidayReason = findViewById(R.id.et_holiday_reason);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        startCal = Calendar.getInstance();
        endCal = Calendar.getInstance();

        btnStartDate.setOnClickListener(v -> showDatePicker(startCal, btnStartDate, true));
        btnEndDate.setOnClickListener(v -> showDatePicker(endCal, btnEndDate, false));

        btnConfirmHoliday.setOnClickListener(v -> declareHoliday(true));
        btnCancelHoliday.setOnClickListener(v -> declareHoliday(false));
    }

    private void fetchFacultyName() {
        if (facultyUid == null) return;

        db.collection("faculty").document(facultyUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                facultyName = documentSnapshot.getString("name");
            } else {
                db.collection("users").document(facultyUid).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists() && userDoc.getString("name") != null) {
                        facultyName = userDoc.getString("name");
                    }
                });
            }
        });
    }

    private void showDatePicker(Calendar cal, Button btn, boolean isStart) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            btn.setText(dateStr);

            String startS = btnStartDate.getText().toString();
            String endS = btnEndDate.getText().toString();
            if (!startS.equals("Start Date") && !endS.equals("End Date")) {
                tvDateRange.setText("Range: " + startS + " to " + endS);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void declareHoliday(boolean isDeclare) {
        String startStr = btnStartDate.getText().toString();
        String endStr = btnEndDate.getText().toString();

        if (startStr.equals("Start Date") || endStr.equals("End Date")) {
            Toast.makeText(this, "Please select both Start and End dates", Toast.LENGTH_SHORT).show();
            return;
        }

        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);
        endCal.set(Calendar.HOUR_OF_DAY, 0); endCal.set(Calendar.MINUTE, 0); endCal.set(Calendar.SECOND, 0); endCal.set(Calendar.MILLISECOND, 0);

        if (startCal.after(endCal)) {
            Toast.makeText(this, "End date cannot be before Start date", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = etHolidayReason.getText().toString().trim();
        if (isDeclare && TextUtils.isEmpty(reason)) {
            etHolidayReason.setError("Please enter a reason");
            return;
        }

        Calendar current = (Calendar) startCal.clone();

        while (!current.after(endCal)) {
            String dateStr = String.format(Locale.getDefault(), "%02d-%02d-%04d",
                    current.get(Calendar.DAY_OF_MONTH), current.get(Calendar.MONTH) + 1, current.get(Calendar.YEAR));

            updateStatus(dateStr, isDeclare ? "Holiday" : "Available", reason);

            if (isDeclare) {
                Map<String, Object> globalHoliday = new HashMap<>();
                globalHoliday.put("reason", reason);
                globalHoliday.put("declaredBy", facultyName);
                db.collection("holidays").document(dateStr).set(globalHoliday);
            } else {
                db.collection("holidays").document(dateStr).delete();
            }

            current.add(Calendar.DATE, 1);
        }

        // Only Toast and Data update remains here.
        // The physical notification is now handled ONLY by your laptop server.
        if (isDeclare) {
            Toast.makeText(this, "Holiday Declared Globally", Toast.LENGTH_SHORT).show();
            syncNotificationToDatabase("Holiday Alert", "Holiday declared by Prof. " + facultyName + ": " + reason);
        } else {
            Toast.makeText(this, "Holiday Cancelled", Toast.LENGTH_SHORT).show();
            syncNotificationToDatabase("Update", "Holiday cancelled by Prof. " + facultyName + ". Classes resumed.");
            etHolidayReason.setText("");
        }
    }

    private void updateStatus(String date, String status, String reason) {
        if (facultyUid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("message", reason);
        data.put("date", date);

        db.collection("faculty").document(facultyUid)
                .collection("availability").document(date)
                .set(data, SetOptions.merge());
    }

    /**
     * This method no longer builds a local notification.
     * It only saves the data to Firestore so the 'Notification History'
     * page stays updated. The actual push notification popup will now
     * only come from your Node.js server.
     */
    private void syncNotificationToDatabase(String title, String message) {
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("date", new Date());
        notifData.put("type", "alert");

        db.collection("public_notifications").add(notifData);
    }
}
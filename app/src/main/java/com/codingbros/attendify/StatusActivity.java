package com.codingbros.attendify;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// © Made by Varad Ubale

public class StatusActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RadioGroup rgStatus;
    private RadioButton rbAvailable, rbNotAvailable;
    private Button btnStartDate, btnEndDate, btnConfirmHoliday, btnCancelHoliday;
    private TextView tvDateRange, tvSelectedDateStatus;
    private TextInputEditText etHolidayReason;
    private View viewStatusDot;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private String facultyUid;
    private String facultyName = "Faculty"; // Default fallback name
    private Calendar startCal, endCal;
    private String selectedDateString;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Fetch Faculty Name immediately
        fetchFacultyName();

        // Init Views
        calendarView = findViewById(R.id.calendarView);
        rgStatus = findViewById(R.id.rg_status);
        rbAvailable = findViewById(R.id.rb_available);
        rbNotAvailable = findViewById(R.id.rb_not_available);
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        btnConfirmHoliday = findViewById(R.id.btn_confirm_holiday);
        btnCancelHoliday = findViewById(R.id.btn_cancel_holiday);
        tvDateRange = findViewById(R.id.tv_date_range);
        etHolidayReason = findViewById(R.id.et_holiday_reason);
        tvSelectedDateStatus = findViewById(R.id.tv_selected_date_status);
        viewStatusDot = findViewById(R.id.view_status_dot);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Initialize Dates
        startCal = Calendar.getInstance();
        endCal = Calendar.getInstance();

        // Calendar Click
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            checkDateStatus(selectedDateString);
        });

        // Set initial selected date
        Calendar today = Calendar.getInstance();
        selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));
        checkDateStatus(selectedDateString);

        // 2. Handle Radio Group (Today's Status - CUSTOM MESSAGES HERE)
        rgStatus.setOnCheckedChangeListener((group, checkedId) -> {
            String todayStr = getTodayDateString();

            if (checkedId == R.id.rb_not_available) {
                // Update DB
                updateStatus(todayStr, "Leave", "Faculty not available today");

                // Send "Not Available" Notification
                String msg = "Prof. " + facultyName + " is not available today, Date: " + todayStr;
                sendNotification("Status Update", msg);

            } else {
                // Update DB
                updateStatus(todayStr, "Available", "");

                // Send "Available" Notification (Added as requested)
                String msg = "Prof. " + facultyName + " is available today, Date: " + todayStr;
                sendNotification("Status Update", msg);
            }
        });

        // Holiday Pickers
        btnStartDate.setOnClickListener(v -> showDatePicker(startCal, btnStartDate, true));
        btnEndDate.setOnClickListener(v -> showDatePicker(endCal, btnEndDate, false));

        // Confirm/Cancel Holiday
        btnConfirmHoliday.setOnClickListener(v -> declareHoliday(true));
        btnCancelHoliday.setOnClickListener(v -> declareHoliday(false));
    }

    // --- NEW: Fetch Name for Notifications ---
    private void fetchFacultyName() {
        db.collection("faculty").document(facultyUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                        facultyName = documentSnapshot.getString("name");
                    }
                });
    }

    private void checkDateStatus(String date) {
        db.collection("faculty").document(facultyUid)
                .collection("calendar_status").document(date)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String status = document.getString("status");
                        String msg = document.getString("message");

                        if ("Leave".equals(status)) {
                            updateStatusUI(Color.RED, "Not Available");
                        } else if ("Holiday".equals(status)) {
                            updateStatusUI(Color.YELLOW, "Holiday: " + msg);
                        } else {
                            updateStatusUI(Color.GREEN, "Available");
                        }
                    } else {
                        updateStatusUI(Color.GREEN, "Available");
                    }
                });
    }

    private void updateStatusUI(int color, String text) {
        viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(color));
        tvSelectedDateStatus.setText("Status: " + text);
    }

    // Purely DB update (No automatic notification here to prevent duplicates)
    private void updateStatus(String date, String status, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("message", reason);
        data.put("date", date);

        db.collection("faculty").document(facultyUid)
                .collection("calendar_status").document(date)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (date.equals(selectedDateString)) {
                        checkDateStatus(date);
                    }
                });
    }

    private void declareHoliday(boolean isDeclare) {
        if (startCal.after(endCal)) {
            Toast.makeText(this, "End date cannot be before Start date", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = etHolidayReason.getText().toString().trim();

        if (isDeclare && TextUtils.isEmpty(reason)) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar current = (Calendar) startCal.clone();
        while (!current.after(endCal)) {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    current.get(Calendar.YEAR), current.get(Calendar.MONTH) + 1, current.get(Calendar.DAY_OF_MONTH));

            if (isDeclare) {
                updateStatus(dateStr, "Holiday", reason);
            } else {
                updateStatus(dateStr, "Available", "");
            }
            current.add(Calendar.DATE, 1);
        }

        if (isDeclare) {
            Toast.makeText(this, "Holiday Declared Successfully", Toast.LENGTH_SHORT).show();
            // Custom Holiday Message
            sendNotification("Holiday Alert", "Holiday declared by Prof. " + facultyName + ": " + reason);
        } else {
            Toast.makeText(this, "Holiday Cancelled Successfully", Toast.LENGTH_SHORT).show();
            sendNotification("Update", "Holiday cancelled by Prof. " + facultyName + ". Classes resumed.");
            etHolidayReason.setText("");
        }
    }

    private void showDatePicker(Calendar cal, Button btn, boolean isStart) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
            btn.setText(dateStr);

            String startS = btnStartDate.getText().toString();
            String endS = btnEndDate.getText().toString();
            if (!startS.contains("Date") && !endS.contains("Date")) {
                tvDateRange.setText("Selected Range: " + startS + " to " + endS);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "status_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Status Updates", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notify_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } else {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }

        // Save to Firestore
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("date", new Date());
        notifData.put("type", "alert");

        db.collection("public_notifications").add(notifData);
    }

    private String getTodayDateString() {
        Calendar today = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));
    }
}
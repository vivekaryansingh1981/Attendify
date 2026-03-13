package com.codingbros.attendify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyAvailabilityActivity extends AppCompatActivity {

    private TextView tvMonthYear, tvSelectedDate;
    private ImageView btnPrevMonth, btnNextMonth, btnBack;
    private RecyclerView recyclerCalendar;
    private RadioGroup radioGroupStatus;
    private RadioButton radioAvailable, radioNotAvailable;

    private TextInputLayout layoutAdjustment;
    private TextInputEditText etAdjustment;
    private Button btnSaveStatus;

    private FirebaseFirestore db;
    private String facultyUid;
    private String facultyName = "Faculty"; // Default fallback

    private Calendar currentDisplayMonth;
    private String currentlySelectedDateStr = "";

    // Store full data map for dates
    private Map<String, Map<String, Object>> availabilityMap = new HashMap<>();
    private Map<String, String> currentMonthDisplayMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_availability);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // --- Fetch Name immediately upon opening ---
        fetchFacultyName();

        // --- Permission Check (From your original StatusActivity) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        currentDisplayMonth = Calendar.getInstance();

        btnBack = findViewById(R.id.btn_back);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        tvMonthYear = findViewById(R.id.tv_month_year);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        recyclerCalendar = findViewById(R.id.recycler_calendar);
        radioGroupStatus = findViewById(R.id.radio_group_status);
        radioAvailable = findViewById(R.id.radio_available);
        radioNotAvailable = findViewById(R.id.radio_not_available);

        layoutAdjustment = findViewById(R.id.layout_adjustment_message);
        etAdjustment = findViewById(R.id.et_adjustment_message);
        btnSaveStatus = findViewById(R.id.btn_save_status);

        recyclerCalendar.setLayoutManager(new GridLayoutManager(this, 7));

        btnBack.setOnClickListener(v -> finish());

        btnPrevMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, -1);
            updateCalendarForMonth();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentDisplayMonth.add(Calendar.MONTH, 1);
            updateCalendarForMonth();
        });

        // Default Date is Today
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        currentlySelectedDateStr = sdf.format(Calendar.getInstance().getTime());
        tvSelectedDate.setText("Status for: " + currentlySelectedDateStr);

        // Toggle Adjustment Box Visibility
        radioGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_not_available) {
                layoutAdjustment.setVisibility(View.VISIBLE);
            } else {
                layoutAdjustment.setVisibility(View.GONE);
                etAdjustment.setText("");
            }
        });

        // Handle Save Click
        btnSaveStatus.setOnClickListener(v -> saveStatusUpdate());

        fetchAvailabilityData();
    }

    // --- Fetches Name so it is ready for the Notification ---
    private void fetchFacultyName() {
        if (facultyUid == null) return;

        // Check Faculty collection
        db.collection("faculty").document(facultyUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.getString("name") != null) {
                facultyName = documentSnapshot.getString("name");
            } else {
                // Check Users collection as a backup
                db.collection("users").document(facultyUid).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists() && userDoc.getString("name") != null) {
                        facultyName = userDoc.getString("name");
                    }
                });
            }
        });
    }

    private void fetchAvailabilityData() {
        if (facultyUid == null) return;

        db.collection("faculty").document(facultyUid).collection("availability").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availabilityMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        availabilityMap.put(doc.getId(), doc.getData());
                    }
                    updateRadioButtonsForDate(currentlySelectedDateStr);
                    updateCalendarForMonth();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading availability", Toast.LENGTH_SHORT).show());
    }

    private void saveStatusUpdate() {
        if (currentlySelectedDateStr.isEmpty() || facultyUid == null) return;

        String status = (radioNotAvailable.isChecked()) ? "Not Available" : "Available";
        String message = etAdjustment.getText().toString().trim();

        if (status.equals("Not Available") && TextUtils.isEmpty(message)) {
            etAdjustment.setError("Please enter adjustment details");
            return;
        }

        // Check previous status so we know if they are canceling an old absence
        String previousStatus = "";
        if (availabilityMap.containsKey(currentlySelectedDateStr)) {
            Map<String, Object> oldData = availabilityMap.get(currentlySelectedDateStr);
            if (oldData != null && oldData.get("status") != null) {
                previousStatus = (String) oldData.get("status");
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        if (status.equals("Not Available")) {
            data.put("adjustment_message", message);
        } else {
            data.put("adjustment_message", "");
        }

        // Save to Database
        String finalPreviousStatus = previousStatus;
        db.collection("faculty").document(facultyUid)
                .collection("availability").document(currentlySelectedDateStr)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    availabilityMap.put(currentlySelectedDateStr, data);
                    updateCalendarForMonth();
                    Toast.makeText(this, "Status saved for " + currentlySelectedDateStr, Toast.LENGTH_SHORT).show();

                    // --- Send Appropriate Notification ---
                    if (status.equals("Not Available")) {
                        String msg = "Prof. " + facultyName + " is unavailable on " + currentlySelectedDateStr + ". Adjustment: " + message;
                        sendNotification("Lecture Adjustment Alert", msg);
                    } else if (status.equals("Available") && "Not Available".equals(finalPreviousStatus)) {
                        String msg = "Prof. " + facultyName + " is now AVAILABLE on " + currentlySelectedDateStr + ". Regular schedule resumes.";
                        sendNotification("Lecture Adjustment Cancelled", msg);
                    }
                });
    }

    // --- YOUR EXACT NOTIFICATION METHOD FROM STATUSACTIVITY ---
    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "status_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Status Updates", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Using standard Android icon for safety
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

        // Save to Firestore (In-App Notification Page)
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("date", new Date());
        notifData.put("type", "alert");

        db.collection("public_notifications").add(notifData);
    }

    private void updateCalendarForMonth() {
        currentMonthDisplayMap.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        for (Map.Entry<String, Map<String, Object>> entry : availabilityMap.entrySet()) {
            try {
                Calendar recordCal = Calendar.getInstance();
                recordCal.setTime(sdf.parse(entry.getKey()));

                if (recordCal.get(Calendar.MONTH) == currentDisplayMonth.get(Calendar.MONTH) &&
                        recordCal.get(Calendar.YEAR) == currentDisplayMonth.get(Calendar.YEAR)) {

                    String day = String.valueOf(recordCal.get(Calendar.DAY_OF_MONTH));
                    String status = (String) entry.getValue().get("status");

                    if ("Not Available".equals(status)) {
                        currentMonthDisplayMap.put(day, "Absent"); // RED
                    } else if ("Present".equals(status)) {
                        currentMonthDisplayMap.put(day, "Present"); // GREEN
                    } else if ("Holiday".equals(status)) {
                        currentMonthDisplayMap.put(day, "Holiday"); // YELLOW
                    }
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

        CalendarAdapter adapter = new CalendarAdapter(days, new HashMap<>(currentMonthDisplayMap));
        recyclerCalendar.setAdapter(adapter);
    }

    private void updateRadioButtonsForDate(String dateStr) {
        radioGroupStatus.setOnCheckedChangeListener(null);

        Map<String, Object> data = availabilityMap.get(dateStr);
        if (data != null && "Not Available".equals(data.get("status"))) {
            radioNotAvailable.setChecked(true);
            layoutAdjustment.setVisibility(View.VISIBLE);

            String msg = (String) data.get("adjustment_message");
            etAdjustment.setText(msg != null ? msg : "");
        } else {
            radioAvailable.setChecked(true);
            layoutAdjustment.setVisibility(View.GONE);
            etAdjustment.setText("");
        }

        radioGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_not_available) {
                layoutAdjustment.setVisibility(View.VISIBLE);
            } else {
                layoutAdjustment.setVisibility(View.GONE);
                etAdjustment.setText("");
            }
        });
    }
}
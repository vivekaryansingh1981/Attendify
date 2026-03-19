package com.codingbros.attendify;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditTimetableActivity extends AppCompatActivity {

    private GridLayout gridTimetable;
    private Button btnSubmit;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private boolean isLocked = false;

    private Map<String, Map<String, String>> timetableData = new HashMap<>();

    // --- NEW: This tracks ONLY the slots the user specifically edited this session ---
    private Map<String, Object> pendingUpdates = new HashMap<>();

    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final int SLOTS_PER_DAY = 8;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_timetable);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        gridTimetable = findViewById(R.id.grid_timetable);
        btnSubmit = findViewById(R.id.btn_submit_timetable);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        setupGrid();
        loadSavedTimetable();
        loadTimetableStatus();

        btnSubmit.setOnClickListener(v -> {
            if (isLocked) {
                unlockTimetable();
            } else {
                submitTimetable();
            }
        });
    }

    private void setupGrid() {
        for (int i = 0; i < SLOTS_PER_DAY; i++) {
            for (int j = 0; j < DAYS.length; j++) {
                Button slotBtn = new Button(this);
                slotBtn.setText("Tap to\nEdit");
                slotBtn.setTextSize(12);
                slotBtn.setGravity(Gravity.CENTER);
                slotBtn.setBackgroundResource(R.drawable.bg_timetable_slot);
                slotBtn.setTextColor(Color.DKGRAY);
                slotBtn.setPadding(4, 4, 4, 4);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(j, 1f);
                params.rowSpec = GridLayout.spec(i, 1f);
                params.setMargins(4, 4, 4, 4);
                slotBtn.setLayoutParams(params);

                final int dayIndex = j;
                final int slotIndex = i;

                slotBtn.setOnClickListener(v -> showAddSubjectDialog(slotBtn, dayIndex, slotIndex));

                gridTimetable.addView(slotBtn);
            }
        }
    }

    private void showAddSubjectDialog(Button slotBtn, int dayIndex, int slotIndex) {
        if (isLocked) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_subject, null);
        builder.setView(dialogView);

        TextInputEditText etTeacherName = dialogView.findViewById(R.id.et_teacher_name);
        TextInputEditText etSubName = dialogView.findViewById(R.id.et_subject_name);
        TextInputEditText etSubAbbr = dialogView.findViewById(R.id.et_subject_abbr);
        TextInputEditText etTimeFrom = dialogView.findViewById(R.id.et_time_from);
        TextInputEditText etTimeTo = dialogView.findViewById(R.id.et_time_to);
        Button btnSave = dialogView.findViewById(R.id.btn_save_subject);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_subject);

        etTimeFrom.setOnClickListener(v -> showTimePicker(etTimeFrom));
        etTimeTo.setOnClickListener(v -> showTimePicker(etTimeTo));

        String key = DAYS[dayIndex] + "_" + slotIndex;

        boolean hasData = timetableData.containsKey(key);
        if (hasData) {
            Map<String, String> data = timetableData.get(key);
            if (data != null) {
                etSubName.setText(data.get("subject_name"));
                etTeacherName.setText(data.get("teacher_name"));
                etSubAbbr.setText(data.get("subject_abbr"));
                etTimeFrom.setText(data.get("time_from"));
                etTimeTo.setText(data.get("time_to"));
            }
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSave.setOnClickListener(v -> {
            String teacher = etTeacherName.getText().toString().trim();
            String name = etSubName.getText().toString().trim();
            String abbr = etSubAbbr.getText().toString().trim();
            String from = etTimeFrom.getText().toString().trim();
            String to = etTimeTo.getText().toString().trim();

            boolean isBreak = name.toLowerCase().contains("break") || abbr.toLowerCase().contains("break");

            if (isBreak) {
                if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
                    Toast.makeText(this, "Please select From and To time for the break", Toast.LENGTH_SHORT).show();
                    return;
                }
                name = TextUtils.isEmpty(name) ? "Break" : name;
                abbr = TextUtils.isEmpty(abbr) ? "BREAK" : abbr;
                teacher = "-";
            } else {
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(teacher) || TextUtils.isEmpty(abbr) || TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (isBreak) {
                String buttonText = "☕ " + abbr.toUpperCase() + "\n" + from + "-" + to;
                slotBtn.setText(buttonText);
                slotBtn.setTextColor(Color.parseColor("#D84315"));
            } else {
                String buttonText = abbr + "\n" + from + "-" + to;
                slotBtn.setText(buttonText);
                slotBtn.setTextColor(Color.BLACK);
            }

            Map<String, String> subjectDetails = new HashMap<>();
            subjectDetails.put("subject_name", name);
            subjectDetails.put("teacher_name", teacher);
            subjectDetails.put("subject_abbr", abbr);
            subjectDetails.put("time_from", from);
            subjectDetails.put("time_to", to);
            subjectDetails.put("is_break", isBreak ? "true" : "false");

            timetableData.put(key, subjectDetails);

            // --- FIXED: Log this specific edit into our tracking map ---
            pendingUpdates.put(key, subjectDetails);

            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            timetableData.remove(key);
            slotBtn.setText("Tap to\nEdit");
            slotBtn.setTextColor(Color.DKGRAY);

            // --- FIXED: Log a deliberate deletion into our tracking map ---
            pendingUpdates.put(key, FieldValue.delete());

            Toast.makeText(this, "Cleared. Click Submit to save.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void showTimePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String amPm;
            int displayHour = hourOfDay;

            if (hourOfDay >= 12) {
                amPm = "PM";
                if (displayHour > 12) displayHour -= 12;
            } else {
                amPm = "AM";
                if (displayHour == 0) displayHour = 12;
            }

            String time = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minuteOfHour, amPm);
            editText.setText(time);

        }, hour, minute, false);

        timePickerDialog.show();
    }

    private void submitTimetable() {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Saving...");

        // --- FIXED: Delta Updates! We only process the slots the user explicitly modified ---
        Map<String, Map<String, Object>> updatesByDay = new HashMap<>();

        for (Map.Entry<String, Object> entry : pendingUpdates.entrySet()) {
            String[] parts = entry.getKey().split("_"); // e.g., "Monday" and "0"
            String day = parts[0];
            String slotKey = "slot_" + parts[1];

            updatesByDay.putIfAbsent(day, new HashMap<>());
            updatesByDay.get(day).put(slotKey, entry.getValue());
        }

        // Send only the localized updates to Firebase
        for (Map.Entry<String, Map<String, Object>> dayEntry : updatesByDay.entrySet()) {
            db.collection("timetable").document(dayEntry.getKey()).set(dayEntry.getValue(), SetOptions.merge());
        }

        // Secure the timetable
        Map<String, Object> status = new HashMap<>();
        status.put("is_timetable_locked", true);

        db.collection("timetable").document("status")
                .set(status, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    isLocked = true;
                    pendingUpdates.clear(); // Wipe the slate clean after successful save
                    updateUIVisibility();
                    Toast.makeText(this, "Global Timetable Updated & Locked!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Time Table");
                    Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSavedTimetable() {
        for (int i = 0; i < DAYS.length; i++) {
            String day = DAYS[i];
            final int dayIndex = i;

            db.collection("timetable").document(day)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            for (int slotIndex = 0; slotIndex < SLOTS_PER_DAY; slotIndex++) {
                                String key = "slot_" + slotIndex;
                                if (documentSnapshot.contains(key)) {
                                    Map<String, String> slotData = (Map<String, String>) documentSnapshot.get(key);
                                    updateButtonUI(dayIndex, slotIndex, slotData);
                                    timetableData.put(DAYS[dayIndex] + "_" + slotIndex, slotData);
                                }
                            }
                        }
                    });
        }
    }

    private void updateButtonUI(int dayIndex, int slotIndex, Map<String, String> data) {
        int childIndex = (slotIndex * DAYS.length) + dayIndex;

        if (childIndex < gridTimetable.getChildCount()) {
            View view = gridTimetable.getChildAt(childIndex);
            if (view instanceof Button) {
                Button btn = (Button) view;
                String abbr = data.get("subject_abbr");
                String from = data.get("time_from");
                String to = data.get("time_to");

                boolean isBreak = "true".equals(data.get("is_break")) ||
                        (abbr != null && abbr.toLowerCase().contains("break")) ||
                        (data.get("subject_name") != null && data.get("subject_name").toLowerCase().contains("break"));

                if (isBreak) {
                    btn.setText("☕ " + (abbr != null ? abbr.toUpperCase() : "BREAK") + "\n" + from + "-" + to);
                    btn.setTextColor(Color.parseColor("#D84315"));
                } else {
                    btn.setText(abbr + "\n" + from + "-" + to);
                    btn.setTextColor(Color.BLACK);
                }
            }
        }
    }

    private void loadTimetableStatus() {
        DocumentReference docRef = db.collection("timetable").document("status");
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("is_timetable_locked")) {
                isLocked = Boolean.TRUE.equals(documentSnapshot.getBoolean("is_timetable_locked"));
            }
            updateUIVisibility();
        });
    }

    private void unlockTimetable() {
        Map<String, Object> status = new HashMap<>();
        status.put("is_timetable_locked", false);

        db.collection("timetable").document("status")
                .set(status, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    isLocked = false;
                    updateUIVisibility();
                    Toast.makeText(this, "Global Timetable Unlocked for Editing.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIVisibility() {
        btnSubmit.setEnabled(true);
        if (isLocked) {
            btnSubmit.setText("Edit Time Table");
        } else {
            btnSubmit.setText("Submit Time Table");
        }
        for (int i = 0; i < gridTimetable.getChildCount(); i++) {
            gridTimetable.getChildAt(i).setEnabled(!isLocked);
        }
    }
}
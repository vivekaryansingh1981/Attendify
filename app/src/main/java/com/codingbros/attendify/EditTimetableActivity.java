package com.codingbros.attendify;

import android.app.AlertDialog;
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
import com.google.firebase.firestore.FieldValue; // Import needed for delete
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditTimetableActivity extends AppCompatActivity {

    private GridLayout gridTimetable;
    private Button btnSubmit;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private String facultyUid;
    private boolean isLocked = false;

    // To store timetable data temporarily
    private Map<String, Map<String, String>> timetableData = new HashMap<>();
    private final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private final int SLOTS_PER_DAY = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_timetable);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
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
        Button btnDelete = dialogView.findViewById(R.id.btn_delete_subject); // Link Delete Button

        String key = DAYS[dayIndex] + "_" + slotIndex;

        // --- CHECK IF DATA EXISTS ---
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
            // Show Delete Button only if there is data to delete
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // --- SAVE BUTTON LOGIC ---
        btnSave.setOnClickListener(v -> {
            String teacher = etTeacherName.getText().toString().trim();
            String name = etSubName.getText().toString().trim();
            String abbr = etSubAbbr.getText().toString().trim();
            String from = etTimeFrom.getText().toString().trim();
            String to = etTimeTo.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(teacher) || TextUtils.isEmpty(abbr) || TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update UI
            String buttonText = abbr + "\n" + from + "-" + to;
            slotBtn.setText(buttonText);
            slotBtn.setTextColor(Color.BLACK);

            // Save to Map
            Map<String, String> subjectDetails = new HashMap<>();
            subjectDetails.put("subject_name", name);
            subjectDetails.put("teacher_name", teacher);
            subjectDetails.put("subject_abbr", abbr);
            subjectDetails.put("time_from", from);
            subjectDetails.put("time_to", to);

            timetableData.put(key, subjectDetails);

            dialog.dismiss();
        });

        // --- DELETE BUTTON LOGIC ---
        btnDelete.setOnClickListener(v -> {
            // 1. Remove from local map
            timetableData.remove(key);

            // 2. Reset UI Button
            slotBtn.setText("Tap to\nEdit");
            slotBtn.setTextColor(Color.DKGRAY);

            Toast.makeText(this, "Cleared. Click Submit to save.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void submitTimetable() {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Saving...");

        for (int i = 0; i < DAYS.length; i++) {
            String day = DAYS[i];
            Map<String, Object> dayData = new HashMap<>();

            for (int j = 0; j < SLOTS_PER_DAY; j++) {
                String key = day + "_" + j;

                // --- CRITICAL CHANGE FOR DELETION ---
                // If map has data, save it.
                // If map does NOT have data (user deleted it), explicitly delete from Firebase.

                if (timetableData.containsKey(key)) {
                    dayData.put("slot_" + j, timetableData.get(key));
                } else {
                    // This tells Firestore: "Delete the field 'slot_0' from this document"
                    dayData.put("slot_" + j, FieldValue.delete());
                }
            }

            db.collection("faculty").document(facultyUid)
                    .collection("timetable").document(day)
                    .set(dayData, SetOptions.merge());
        }

        Map<String, Object> status = new HashMap<>();
        status.put("is_timetable_locked", true);
        db.collection("faculty").document(facultyUid)
                .set(status, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    isLocked = true;
                    updateUIVisibility();
                    Toast.makeText(this, "Timetable Updated & Locked!", Toast.LENGTH_SHORT).show();
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

            db.collection("faculty").document(facultyUid)
                    .collection("timetable").document(day)
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
        View view = gridTimetable.getChildAt(childIndex);
        if (view instanceof Button) {
            Button btn = (Button) view;
            String abbr = data.get("subject_abbr");
            String from = data.get("time_from");
            String to = data.get("time_to");
            String buttonText = abbr + "\n" + from + "-" + to;
            btn.setText(buttonText);
            btn.setTextColor(Color.BLACK);
        }
    }

    private void loadTimetableStatus() {
        DocumentReference docRef = db.collection("faculty").document(facultyUid);
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
        db.collection("faculty").document(facultyUid)
                .set(status, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    isLocked = false;
                    updateUIVisibility();
                    Toast.makeText(this, "Timetable Unlocked for Editing.", Toast.LENGTH_SHORT).show();
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
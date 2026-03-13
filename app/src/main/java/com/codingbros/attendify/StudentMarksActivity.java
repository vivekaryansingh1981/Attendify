package com.codingbros.attendify;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentMarksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MarksAdapter adapter;
    private List<Map<String, String>> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private Button btnSubmit;
    private EditText etExamName, etTotalMarks;
    private TextView tvSubjectTitle;
    private Toolbar toolbar;

    private String subjectName;
    private boolean isLocked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_marks);

        // --- FIXED: Get BOTH the full name (for DB) and abbreviation (for UI) ---
        subjectName = getIntent().getStringExtra("subject_name");
        String subjectAbbr = getIntent().getStringExtra("subject_abbr");

        // Fallback in case abbreviation is null
        if (subjectAbbr == null || subjectAbbr.trim().isEmpty()) {
            subjectAbbr = subjectName;
        }

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        recyclerView = findViewById(R.id.recycler_marks);
        btnSubmit = findViewById(R.id.btn_submit_marks);
        etExamName = findViewById(R.id.et_exam_name);
        etTotalMarks = findViewById(R.id.et_total_marks);
        tvSubjectTitle = findViewById(R.id.tv_subject_title);
        ImageView btnBack = findViewById(R.id.btn_back);

        // --- FIXED: Display the abbreviation in the title ---
        tvSubjectTitle.setText("Enter Marks: " + subjectAbbr);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarksAdapter(studentList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadStudents();

        btnSubmit.setOnClickListener(v -> {
            if (isLocked) {
                unlockMarks();
            } else {
                saveMarks();
            }
        });
    }

    // --- MENU SYSTEM ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_marks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            clearForm();
            return true;
        } else if (id == R.id.action_history) {
            showHistoryDialog();
            return true;
        } else if (id == R.id.action_delete_current) {
            confirmDeleteCurrent();
            return true;
        } else if (id == R.id.action_delete_all) {
            confirmDeleteAll();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // 1. Refresh / Clear Data
    private void clearForm() {
        etExamName.setText("");
        etTotalMarks.setText("");
        adapter.setMarksState(new HashMap<>());
        isLocked = false;
        updateLockButtonUI();
        Toast.makeText(this, "Form Cleared", Toast.LENGTH_SHORT).show();
    }

    // 2. See Previous Data (History)
    private void showHistoryDialog() {
        db.collection("marks")
                .whereEqualTo("subject", subjectName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No previous data found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> displayNames = new ArrayList<>();
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                    for (DocumentSnapshot doc : docs) {
                        String name = doc.getString("exam_name");
                        Long timestamp = doc.getLong("timestamp");
                        String dateStr = timestamp != null ? sdf.format(new Date(timestamp)) : "Unknown";
                        displayNames.add(name + "\n(" + dateStr + ")");
                    }

                    String[] namesArray = displayNames.toArray(new String[0]);

                    new AlertDialog.Builder(this)
                            .setTitle("Select Previous Exam")
                            .setItems(namesArray, (dialog, which) -> {
                                loadMarksFromDoc(docs.get(which));
                            })
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading history. Check Logcat.", Toast.LENGTH_LONG).show();
                    Log.e("FirestoreError", e.getMessage());
                });
    }

    // 3. Delete Current Data Logic
    private void confirmDeleteCurrent() {
        String examName = etExamName.getText().toString().trim();

        if (TextUtils.isEmpty(examName)) {
            Toast.makeText(this, "Please enter or select an Exam Name to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Current Exam?")
                .setMessage("Are you sure you want to delete the record for '" + examName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCurrentExam(examName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCurrentExam(String examName) {
        String docId = subjectName + "_" + examName.replaceAll("\\s+", "_");

        db.collection("marks").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 4. Delete All Data
    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("⚠ Delete ALL History?")
                .setMessage("Warning: This will permanently delete ALL records (Unit Tests, Finals, etc.) for " + subjectName + ". This cannot be undone.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("DELETE ALL", (dialog, which) -> deleteAllData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllData() {
        db.collection("marks")
                .whereEqualTo("subject", subjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "All History Deleted", Toast.LENGTH_SHORT).show();
                        clearForm();
                    });
                });
    }

    // --- LOADING LOGIC ---
    private void loadStudents() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                studentList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, String> student = new HashMap<>();
                    student.put("uid", document.getId());
                    student.put("name", document.getString("name"));
                    Object enrollObj = document.get("enrollment");
                    student.put("enrollment", enrollObj != null ? String.valueOf(enrollObj) : "0");
                    studentList.add(student);
                }

                java.util.Collections.sort(studentList, (s1, s2) -> {
                    String e1 = s1.get("enrollment");
                    String e2 = s2.get("enrollment");
                    try { return Long.valueOf(e1).compareTo(Long.valueOf(e2)); }
                    catch (NumberFormatException e) { return e1.compareTo(e2); }
                });

                adapter.notifyDataSetChanged();
                fetchLatestMarks();
            }
        });
    }

    private void fetchLatestMarks() {
        db.collection("marks")
                .whereEqualTo("subject", subjectName)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        loadMarksFromDoc(doc);
                        Toast.makeText(this, "Loaded: " + doc.getString("exam_name"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMarksFromDoc(DocumentSnapshot doc) {
        etExamName.setText(doc.getString("exam_name"));
        etTotalMarks.setText(doc.getString("total_marks"));

        Map<String, String> marksData = (Map<String, String>) doc.get("marks_data");
        if (marksData != null) {
            adapter.setMarksState(marksData);
        }

        if (doc.contains("isLocked")) {
            isLocked = Boolean.TRUE.equals(doc.getBoolean("isLocked"));
            updateLockButtonUI();
        }
    }

    // --- SAVING LOGIC ---
    private void saveMarks() {
        String examName = etExamName.getText().toString().trim();
        String totalMarks = etTotalMarks.getText().toString().trim();
        Map<String, String> marksData = adapter.getMarksState();

        if (TextUtils.isEmpty(examName) || TextUtils.isEmpty(totalMarks)) {
            Toast.makeText(this, "Please enter Exam Name and Total Marks", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Saving...");

        String docId = subjectName + "_" + examName.replaceAll("\\s+", "_");

        Map<String, Object> finalData = new HashMap<>();
        finalData.put("subject", subjectName);
        finalData.put("exam_name", examName);
        finalData.put("total_marks", totalMarks);
        finalData.put("marks_data", marksData);
        finalData.put("isLocked", true);
        finalData.put("timestamp", System.currentTimeMillis());

        db.collection("marks").document(docId)
                .set(finalData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Marks Saved Successfully", Toast.LENGTH_SHORT).show();
                    isLocked = true;
                    updateLockButtonUI();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Marks");
                });
    }

    private void unlockMarks() {
        isLocked = false;
        updateLockButtonUI();
    }

    private void updateLockButtonUI() {
        btnSubmit.setEnabled(true);
        adapter.setLocked(isLocked);
        etExamName.setEnabled(!isLocked);
        etTotalMarks.setEnabled(!isLocked);

        if (isLocked) {
            btnSubmit.setText("Edit Marks");
            btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            btnSubmit.setText("Submit Marks");
            btnSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2C969E")));
        }
    }
}
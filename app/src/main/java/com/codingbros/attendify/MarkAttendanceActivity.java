package com.codingbros.attendify;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MarkAttendanceActivity extends AppCompatActivity {

    private LinearLayout containerSubjects;
    private FirebaseFirestore db;
    private String facultyUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        containerSubjects = findViewById(R.id.container_subjects);
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnAddSubject = findViewById(R.id.btn_add_subject);

        btnBack.setOnClickListener(v -> finish());
        btnAddSubject.setOnClickListener(v -> showAddSubjectDialog());

        loadSubjects();
    }

    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Subject");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final TextInputEditText inputName = new TextInputEditText(this);
        inputName.setHint("Subject Name (e.g. Physics)");
        layout.addView(inputName);

        final TextInputEditText inputAbbr = new TextInputEditText(this);
        inputAbbr.setHint("Abbreviation (e.g. PHY)");
        layout.addView(inputAbbr);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            String abbr = inputAbbr.getText().toString().trim();

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(abbr)) {
                saveSubject(name, abbr);
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveSubject(String name, String abbr) {
        Map<String, Object> subject = new HashMap<>();
        subject.put("name", name);
        subject.put("abbr", abbr);

        db.collection("faculty").document(facultyUid)
                .collection("subjects").add(subject)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Subject Added", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error adding subject", Toast.LENGTH_SHORT).show());
    }

    private void loadSubjects() {
        db.collection("faculty").document(facultyUid)
                .collection("subjects")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        containerSubjects.removeAllViews();

                        if(task.getResult().isEmpty()){
                            TextView tv = new TextView(this);
                            tv.setText("No subjects added yet.\nClick + to add.");
                            tv.setPadding(50,50,50,50);
                            containerSubjects.addView(tv);
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String abbr = document.getString("abbr");
                            // Pass the Document ID so we know what to delete
                            addSubjectView(name, abbr, document.getId());
                        }
                    }
                });
    }

    private void addSubjectView(String name, String abbr, String docId) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_subject_card, containerSubjects, false);

        TextView tvName = view.findViewById(R.id.tv_subject_name);
        TextView tvAbbr = view.findViewById(R.id.tv_subject_abbr);

        tvName.setText(name);
        tvAbbr.setText(abbr);

        // Regular Click: Open Attendance (To be implemented)
        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentListActivity.class);
            intent.putExtra("subject_name", name); // Pass the subject name
            startActivity(intent);
            Toast.makeText(this, "Clicked: " + name, Toast.LENGTH_SHORT).show();
        });

        // --- NEW: LONG PRESS TO DELETE ---
        view.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Subject")
                    .setMessage("Are you sure you want to delete " + name + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteSubject(docId);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Return true to indicate the click was handled
        });

        containerSubjects.addView(view);
    }

    // Helper method to delete from Firestore
    private void deleteSubject(String docId) {
        db.collection("faculty").document(facultyUid)
                .collection("subjects").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Subject Deleted", Toast.LENGTH_SHORT).show();
                    loadSubjects(); // Refresh list to remove the item
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error deleting subject", Toast.LENGTH_SHORT).show()
                );
    }
}
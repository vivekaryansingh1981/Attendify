package com.codingbros.attendify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FacultySendNoticeActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDesc;
    private Button btnPublish, btnManageNotices;
    private ImageView btnBack;
    private TextView tvComposeTitle, tvPageMainTitle;

    private FirebaseFirestore db;
    private String facultyUid;
    private String facultyName = "Faculty";

    // Edit Mode Variables
    private String editNoticeId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_send_notice);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            facultyUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        fetchFacultyName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        etTitle = findViewById(R.id.et_notice_title);
        etDesc = findViewById(R.id.et_notice_desc);
        btnPublish = findViewById(R.id.btn_publish_notice);
        btnManageNotices = findViewById(R.id.btn_manage_notices);
        btnBack = findViewById(R.id.btn_back);
        tvComposeTitle = findViewById(R.id.tv_compose_title);
        tvPageMainTitle = findViewById(R.id.tv_page_main_title);

        btnBack.setOnClickListener(v -> finish());
        btnPublish.setOnClickListener(v -> publishNotice());

        btnManageNotices.setOnClickListener(v -> {
            startActivity(new Intent(FacultySendNoticeActivity.this, FacultyManageNoticesActivity.class));
        });

        // --- NEW: Check if opened in Edit Mode ---
        editNoticeId = getIntent().getStringExtra("notice_id");
        if (editNoticeId != null) {
            etTitle.setText(getIntent().getStringExtra("notice_title"));
            etDesc.setText(getIntent().getStringExtra("notice_desc"));

            btnPublish.setText("Update Notice");
            tvComposeTitle.setText("Edit Existing Notice");
            tvPageMainTitle.setText("Edit Notice");
            btnManageNotices.setVisibility(View.GONE); // Hide manage button while editing
        }
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

    private void publishNotice() {
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            return;
        }

        if (TextUtils.isEmpty(desc)) {
            etDesc.setError("Description is required");
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText("Saving...");

        if (editNoticeId != null) {
            // --- EDIT EXISTING NOTICE ---
            db.collection("notices").document(editNoticeId)
                    .update("title", title, "description", desc)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Notice Updated Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Update Notice");
                        Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // --- CREATE NEW NOTICE ---
            Map<String, Object> notice = new HashMap<>();
            notice.put("title", title);
            notice.put("description", desc);
            notice.put("author", "Prof. " + facultyName);
            notice.put("timestamp", System.currentTimeMillis());

            db.collection("notices").add(notice)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Notice Published Successfully!", Toast.LENGTH_SHORT).show();
                        sendNotification("New Notice: " + title, "Posted by Prof. " + facultyName);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnPublish.setEnabled(true);
                        btnPublish.setText("Publish Notice");
                        Toast.makeText(this, "Failed to publish notice.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "status_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Status Updates", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("date", new Date());
        notifData.put("type", "alert");
        db.collection("public_notifications").add(notifData);
    }
}
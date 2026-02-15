package com.codingbros.attendify;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout containerNotifications;
    private FirebaseFirestore db;
    private ImageView btnClearAll;
    private String currentUserId;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        // Get User ID to ensure we clear ONLY for this user
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            currentUserId = "guest";
        }

        // Init SharedPrefs
        sharedPreferences = getSharedPreferences("AttendifyNotifs", Context.MODE_PRIVATE);

        containerNotifications = findViewById(R.id.container_notifications);
        ImageView btnBack = findViewById(R.id.btn_back);
        btnClearAll = findViewById(R.id.btn_clear_all); // Link new button

        btnBack.setOnClickListener(v -> finish());

        // --- CLEAR ALL LOGIC ---
        btnClearAll.setOnClickListener(v -> {
            // Save current time as the "Last Cleared" timestamp
            long currentTime = System.currentTimeMillis();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("last_cleared_" + currentUserId, currentTime);
            editor.apply();

            // Clear UI immediately
            containerNotifications.removeAllViews();
            showEmptyMessage("Notifications cleared.");
            Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
        });

        loadNotifications();
    }

    private void loadNotifications() {
        // 1. Get the last time this specific user cleared notifications
        // Default is 0 (show everything if never cleared)
        long lastClearedTime = sharedPreferences.getLong("last_cleared_" + currentUserId, 0);

        db.collection("public_notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        containerNotifications.removeAllViews();

                        boolean hasNewItems = false;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String message = document.getString("message");
                            Date date = document.getDate("date");

                            // --- FILTER LOGIC ---
                            // Only show if the notification is NEWER than the last cleared time
                            if (date != null && date.getTime() > lastClearedTime) {
                                addNotificationCard(title, message, date);
                                hasNewItems = true;
                            }
                        }

                        if (!hasNewItems) {
                            showEmptyMessage("No new notifications");
                        }
                    } else {
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addNotificationCard(String title, String message, Date date) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);
        card.setRadius(12f);
        card.setCardElevation(4f);
        card.setContentPadding(24, 24, 24, 24);
        card.setCardBackgroundColor(getResources().getColor(android.R.color.white));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(getResources().getColor(android.R.color.black));

        // Message
        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tvMsg.setPadding(0, 8, 0, 8);

        // Date
        TextView tvDate = new TextView(this);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
        tvDate.setText(date != null ? sdf.format(date) : "Just now");
        tvDate.setTextSize(12);
        tvDate.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));

        innerLayout.addView(tvTitle);
        innerLayout.addView(tvMsg);
        innerLayout.addView(tvDate);
        card.addView(innerLayout);

        containerNotifications.addView(card);
    }

    private void showEmptyMessage(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
        tv.setPadding(20, 20, 20, 20);
        containerNotifications.addView(tv);
    }
}
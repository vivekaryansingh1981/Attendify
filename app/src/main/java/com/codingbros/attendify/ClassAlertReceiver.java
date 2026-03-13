package com.codingbros.attendify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ClassAlertReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "class_alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        String subjectName = intent.getStringExtra("subject_name");
        String timeFrom = intent.getStringExtra("time_from");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Class Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming classes");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Tap action to open the app
        Intent tapIntent = new Intent(context, FacultydashboardActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure you have an icon here, or use android.R.drawable.ic_dialog_info
                .setContentTitle("Upcoming Class: " + subjectName)
                .setContentText("Your " + subjectName + " class starts at " + timeFrom + " (in 5 minutes!)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
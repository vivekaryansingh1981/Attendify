package com.codingbros.attendify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RoleSelectionActivity extends AppCompatActivity {

    private LinearLayout btnStudent, btnFaculty, btnParent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role); // Your role layout filename

        // --- THE FIX: Ask EVERYONE for Notification Permission right here! ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        btnStudent = findViewById(R.id.btn_student);
        btnFaculty = findViewById(R.id.btn_faculty);
        btnParent = findViewById(R.id.btn_parent);

        btnStudent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, StudentLoginActivity.class);
            intent.putExtra("role", "student");
            startActivity(intent);
        });

        btnFaculty.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, FacultyLoginActivity.class);
            intent.putExtra("role", "faculty");
            startActivity(intent);
        });

        btnParent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, ParentLoginActivity.class);
            intent.putExtra("role", "parent");
            startActivity(intent);
        });
    }
}
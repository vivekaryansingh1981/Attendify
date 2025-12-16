package com.codingbros.attendify;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    private LinearLayout btnStudent, btnFaculty, btnParent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role); // Your role layout filename

        btnStudent = findViewById(R.id.btn_student);
        btnFaculty = findViewById(R.id.btn_faculty);
        btnParent = findViewById(R.id.btn_parent);

        btnStudent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("role", "student");
            startActivity(intent);
        });

        btnFaculty.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("role", "faculty");
            startActivity(intent);
        });

        btnParent.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("role", "parent");
            startActivity(intent);
        });
    }
}

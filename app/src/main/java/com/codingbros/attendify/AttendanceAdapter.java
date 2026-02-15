package com.codingbros.attendify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<Map<String, String>> studentList;
    private boolean isLocked = false;

    // Stores attendance state: Key = UserUID, Value = "Present" or "Absent"
    private Map<String, String> attendanceState = new HashMap<>();

    public AttendanceAdapter(List<Map<String, String>> studentList) {
        this.studentList = studentList;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
        notifyDataSetChanged();
    }

    // Method to get final data for saving
    public Map<String, String> getAttendanceState() {
        return attendanceState;
    }

    // Method to load existing data
    public void setAttendanceState(Map<String, String> loadedState) {
        this.attendanceState = loadedState;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> student = studentList.get(position);
        String uid = student.get("uid");
        String name = student.get("name");
        String enroll = student.get("enrollment");

        // Simple Roll No logic (Position + 1) or fetch from DB if you have it
        holder.tvRoll.setText(String.valueOf(position + 1));
        holder.tvEnroll.setText(enroll != null ? enroll : "N/A");
        holder.tvName.setText(name);

        // Handle Locking
        holder.rbPresent.setEnabled(!isLocked);
        holder.rbAbsent.setEnabled(!isLocked);

        // Clear previous listener to avoid glitches while scrolling
        holder.rgAttendance.setOnCheckedChangeListener(null);

        // Set State based on map
        if (attendanceState.containsKey(uid)) {
            String status = attendanceState.get(uid);
            if ("Present".equals(status)) {
                holder.rbPresent.setChecked(true);
            } else if ("Absent".equals(status)) {
                holder.rbAbsent.setChecked(true);
            }
        } else {
            // Default state: None checked or Mark Present by default if you prefer
            holder.rgAttendance.clearCheck();
        }

        // Add Listener
        holder.rgAttendance.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_present) {
                attendanceState.put(uid, "Present");
            } else if (checkedId == R.id.rb_absent) {
                attendanceState.put(uid, "Absent");
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoll, tvEnroll, tvName;
        RadioGroup rgAttendance;
        RadioButton rbPresent, rbAbsent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoll = itemView.findViewById(R.id.tv_roll_no);
            tvEnroll = itemView.findViewById(R.id.tv_enrollment);
            tvName = itemView.findViewById(R.id.tv_student_name);
            rgAttendance = itemView.findViewById(R.id.rg_attendance);
            rbPresent = itemView.findViewById(R.id.rb_present);
            rbAbsent = itemView.findViewById(R.id.rb_absent);
        }
    }
}
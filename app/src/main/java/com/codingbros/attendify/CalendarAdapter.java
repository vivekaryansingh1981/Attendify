package com.codingbros.attendify;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private List<String> days;
    private Map<String, String> statusMap;

    public CalendarAdapter(List<String> days, Map<String, String> statusMap) {
        this.days = days;
        this.statusMap = statusMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String day = days.get(position);
        holder.tvDay.setText(day);

        // Hide empty slots (days before the 1st of the month)
        if (day.isEmpty()) {
            holder.container.setVisibility(View.INVISIBLE);
            return;
        } else {
            holder.container.setVisibility(View.VISIBLE);
        }

        // --- Logic for Colors ---
        if (statusMap.containsKey(day)) {
            String status = statusMap.get(day);

            if ("Present".equals(status)) {
                // Green Box, White Text
                holder.container.setBackgroundResource(R.drawable.bg_cal_box_present);
                holder.tvDay.setTextColor(Color.WHITE);
            }
            else if ("Absent".equals(status)) {
                // Red Box, White Text
                holder.container.setBackgroundResource(R.drawable.bg_cal_box_absent);
                holder.tvDay.setTextColor(Color.WHITE);
            }
        } else {
            // Default: White Box, Black Text
            holder.container.setBackgroundResource(R.drawable.bg_cal_box_default);
            holder.tvDay.setTextColor(Color.parseColor("#333333"));
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        LinearLayout container; // Reference to the box container

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_cal_day);
            container = itemView.findViewById(R.id.container_day);
        }
    }
}

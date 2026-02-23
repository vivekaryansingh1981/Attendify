package com.codingbros.attendify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class StudentSubjectAdapter extends RecyclerView.Adapter<StudentSubjectAdapter.ViewHolder> {

    private List<Map<String, String>> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(String subjectName);
    }

    public StudentSubjectAdapter(List<Map<String, String>> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse your existing item_subject_card.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);
        holder.tvName.setText(item.get("name"));
        holder.tvAbbr.setText(item.get("abbr"));

        holder.itemView.setOnClickListener(v -> listener.onClick(item.get("name")));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAbbr;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_subject_name);
            tvAbbr = itemView.findViewById(R.id.tv_subject_abbr);
        }
    }
}
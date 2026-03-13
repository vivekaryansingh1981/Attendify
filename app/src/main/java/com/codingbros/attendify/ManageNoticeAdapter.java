package com.codingbros.attendify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ManageNoticeAdapter extends RecyclerView.Adapter<ManageNoticeAdapter.ViewHolder> {

    private List<Map<String, String>> list;
    private OnNoticeClickListener listener;

    public interface OnNoticeClickListener {
        void onEdit(Map<String, String> notice);
        void onDelete(String noticeId, int position);
    }

    public ManageNoticeAdapter(List<Map<String, String>> list, OnNoticeClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_notice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> item = list.get(position);
        holder.tvTitle.setText(item.get("title"));
        holder.tvDesc.setText(item.get("description"));
        holder.tvDate.setText(item.get("date"));

        // NEW: Set Author Name
        holder.tvAuthor.setText(item.get("author"));

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item.get("id"), position));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvDate, tvAuthor; // Added tvAuthor
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notice_title);
            tvDesc = itemView.findViewById(R.id.tv_notice_desc);
            tvDate = itemView.findViewById(R.id.tv_notice_date);
            tvAuthor = itemView.findViewById(R.id.tv_notice_author); // Mapped new ID
            btnEdit = itemView.findViewById(R.id.btn_edit_notice);
            btnDelete = itemView.findViewById(R.id.btn_delete_notice);
        }
    }
}
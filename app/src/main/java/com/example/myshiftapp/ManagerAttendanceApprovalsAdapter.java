package com.example.myshiftapp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManagerAttendanceApprovalsAdapter
        extends RecyclerView.Adapter<ManagerAttendanceApprovalsAdapter.VH> {

    public interface OnApproveClick {
        void onApprove(ApprovalItem item);
    }

    private final List<ApprovalItem> items;
    private final OnApproveClick onApproveClick;

    public ManagerAttendanceApprovalsAdapter(List<ApprovalItem> items, OnApproveClick onApproveClick) {
        this.items = items;
        this.onApproveClick = onApproveClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ApprovalItem it = items.get(position);

// Title: Name | ShiftType | Status
        h.tvTitle.setText(String.format(
                Locale.getDefault(),
                "%s | %s | %s",
                safe(it.employeeName),
                safe(it.shiftType),
                safe(it.status)
        ));

        String startTxt = DateFormat.format("dd/MM HH:mm", new Date(it.startTimeMillis)).toString();
        String endTxt = DateFormat.format("dd/MM HH:mm", new Date(it.endTimeMillis)).toString();

        long hours = it.durationMinutes / 60;
        long mins = it.durationMinutes % 60;

        h.tvDetails.setText(String.format(
                Locale.getDefault(),
                "%s - %s | Total: %dh %02dm",
                startTxt, endTxt, hours, mins
        ));

        h.btnApprove.setOnClickListener(v -> {
            if (onApproveClick != null) onApproveClick.onApprove(it);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails;
        Button btnApprove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            btnApprove = itemView.findViewById(R.id.btnApprove);
        }
    }
}
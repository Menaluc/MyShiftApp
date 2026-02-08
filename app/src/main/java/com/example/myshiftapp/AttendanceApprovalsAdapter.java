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

public class AttendanceApprovalsAdapter
        extends RecyclerView.Adapter<AttendanceApprovalsAdapter.VH> {

    public interface OnApproveClick {
        void onApprove(AttendanceRecord item);
    }

    private final List<AttendanceRecord> items;
    private final OnApproveClick onApproveClick;

    public AttendanceApprovalsAdapter(List<AttendanceRecord> items,
                                      OnApproveClick onApproveClick) {
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
        AttendanceRecord it = items.get(position);

// Title: shift type + status
        h.tvTitle.setText(
                String.format(
                        Locale.getDefault(),
                        "Shift: %s | Status: %s",
                        it.shiftType,
                        it.status
                )
        );

// Format times
        String startTxt = DateFormat.format("HH:mm",
                new Date(it.startTimeMillis)).toString();

        String endTxt = DateFormat.format("HH:mm",
                new Date(it.endTimeMillis)).toString();

        long hours = it.durationMinutes / 60;
        long mins = it.durationMinutes % 60;

        h.tvDetails.setText(
                String.format(
                        Locale.getDefault(),
                        "%s - %s | Total: %dh %02dm",
                        startTxt,
                        endTxt,
                        hours,
                        mins
                )
        );

        h.btnApprove.setOnClickListener(v -> onApproveClick.onApprove(it));
    }

    @Override
    public int getItemCount() {
        return items.size();
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
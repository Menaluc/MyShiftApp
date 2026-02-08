package com.example.myshiftapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceRecordAdapter extends RecyclerView.Adapter<AttendanceRecordAdapter.VH> {

    private final List<AttendanceRecord> items;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AttendanceRecordAdapter(List<AttendanceRecord> items) {
        this.items = items;
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
        AttendanceRecord r = items.get(position);

        String line1 = safe(r.shiftType) + " | " + r.durationMinutes + " min";
        String line2 = "Start: " + timeFmt.format(new Date(r.startTimeMillis))
                + " End: " + timeFmt.format(new Date(r.endTimeMillis));
        String line3 = "Status: " + safe(r.status);

        h.tvLine1.setText(line1);
        h.tvLine2.setText(line2);
        h.tvLine3.setText(line3);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLine1, tvLine2, tvLine3;
        VH(@NonNull View itemView) {
            super(itemView);
            tvLine1 = itemView.findViewById(R.id.tvTitle);
            tvLine2 = itemView.findViewById(R.id.tvDetails);
            tvLine3 = itemView.findViewById(R.id.btnApprove);
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
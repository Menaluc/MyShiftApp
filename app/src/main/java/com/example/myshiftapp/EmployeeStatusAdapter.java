
package com.example.myshiftapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmployeeStatusAdapter extends RecyclerView.Adapter<EmployeeStatusAdapter.VH> {

    private final List<EmployeeStatus> items;

    public EmployeeStatusAdapter(List<EmployeeStatus> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_status, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EmployeeStatus it = items.get(position);

        h.tvName.setText(it.fullName);
        h.tvStatus.setText("Submitted: " + (it.submitted ? "YES" : "NO"));
        h.tvSubmittedAt.setText("SubmittedAt: " + (it.submittedAtText == null ? "-" : it.submittedAtText));

        int bgColor = it.submitted
                ? ContextCompat.getColor(h.itemView.getContext(), R.color.status_green)
                : ContextCompat.getColor(h.itemView.getContext(), R.color.status_red);

        h.itemView.setBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvSubmittedAt;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSubmittedAt = itemView.findViewById(R.id.tvSubmittedAt);
        }
    }
}

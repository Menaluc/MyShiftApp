package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmployeeScheduleFragment extends Fragment {

    private FirebaseFirestore db;

    private TextView tvConfigSummary, tvSubmitStatus;
    private Button btnOpenAvailability, btnBack;

    public EmployeeScheduleFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_employee_schedule, container, false);

        tvConfigSummary = view.findViewById(R.id.tvConfigSummary);
        tvSubmitStatus = view.findViewById(R.id.tvSubmitStatus);
        btnOpenAvailability = view.findViewById(R.id.btnOpenAvailability);
        btnBack = view.findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();

        // Back -> return to previous screen
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Next step: availability screen (for now placeholder)
        btnOpenAvailability.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Open Availability (next step)", Toast.LENGTH_SHORT).show()
        );

        loadShiftConfig();

        return view;
    }

    private void loadShiftConfig() {
        db.collection("settings").document("shift_config")
                .get()
                .addOnSuccessListener(this::handleConfig)
                .addOnFailureListener(e -> {
                    tvConfigSummary.setText("Failed to load settings.");
                    tvSubmitStatus.setText(e.getMessage());
                    btnOpenAvailability.setEnabled(false);
                });
    }

    private void handleConfig(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            tvConfigSummary.setText("No settings found. Ask manager to configure shifts.");
            tvSubmitStatus.setText("");
            btnOpenAvailability.setEnabled(false);
            return;
        }

        Boolean canSubmit = doc.getBoolean("canSubmitConstraints"); // matches your Firestore
        Long employeesPerShift = doc.getLong("employeesPerShift");  // number
        String eveningStart = doc.getString("eveningStart");
        String eveningEnd = doc.getString("eveningEnd");
        String morningStart = doc.getString("morningStart");
        String morningEnd = doc.getString("morningEnd");
        String nightStart = doc.getString("nightStart");
        String nightEnd = doc.getString("nightEnd");
        String shiftType = doc.getString("shiftType");             // "2" / "3"
        String workDays = doc.getString("workDays");               // "SunThu"

        if (canSubmit == null) canSubmit = false;
        if (shiftType == null) shiftType = "2";

        StringBuilder sb = new StringBuilder();

        sb.append("Shift type: ").append(shiftType).append(" shifts").append("\n\n");

        sb.append("Morning: ")
                .append(nullSafe(morningStart)).append(" - ").append(nullSafe(morningEnd))
                .append("\n");

        sb.append("Evening: ")
                .append(nullSafe(eveningStart)).append(" - ").append(nullSafe(eveningEnd))
                .append("\n");

        if ("3".equals(shiftType)) {
            sb.append("Night: ")
                    .append(nullSafe(nightStart)).append(" - ").append(nullSafe(nightEnd))
                    .append("\n");
        }

        sb.append("\nEmployees per shift: ")
                .append(employeesPerShift == null ? "?" : employeesPerShift)
                .append("\n");

        sb.append("Work days: ").append(nullSafe(workDays)).append("\n");

        tvConfigSummary.setText(sb.toString());

        if (canSubmit) {
            tvSubmitStatus.setText("Availability submission is OPEN ");
            btnOpenAvailability.setEnabled(true);
        } else {
            tvSubmitStatus.setText("Availability submission is CLOSED (waiting for manager)");
            btnOpenAvailability.setEnabled(false);
        }
    }

    private String nullSafe(String s) {
        return (s == null) ? "" : s;
    }
}

package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EmployeeScheduleFragment extends Fragment {

    private FirebaseFirestore db;

    private TextView tvConfigSummary, tvSubmitStatus;
    private Button btnOpenAvailability, btnBack;

    public EmployeeScheduleFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_employee_schedule, container, false);

        tvConfigSummary = view.findViewById(R.id.tvConfigSummary);
        tvSubmitStatus = view.findViewById(R.id.tvSubmitStatus);
        btnOpenAvailability = view.findViewById(R.id.btnOpenAvailability);
        btnBack = view.findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // ✅ מעבר לטבלת הזמינות (ShiftScheduleFragment)
        btnOpenAvailability.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.shiftScheduleFragment);
        });

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

        Boolean canSubmit = doc.getBoolean("canSubmitConstraints");
        Long employeesPerShift = doc.getLong("employeesPerShift");
        String shiftType = doc.getString("shiftType");   // "2" / "3"
        String workDays  = doc.getString("workDays");    // SunThu / SunFri / SunSat

        String morningStart = doc.getString("morningStart");
        String morningEnd   = doc.getString("morningEnd");
        String eveningStart = doc.getString("eveningStart");
        String eveningEnd   = doc.getString("eveningEnd");
        String nightStart   = doc.getString("nightStart");
        String nightEnd     = doc.getString("nightEnd");

        if (canSubmit == null) canSubmit = false;
        if (shiftType == null) shiftType = "2";
        if (workDays == null) workDays = "SunThu";

        StringBuilder sb = new StringBuilder();
        sb.append("Shift type: ").append(shiftType).append("\n\n");

        sb.append("Morning: ").append(ns(morningStart)).append(" - ").append(ns(morningEnd)).append("\n");
        sb.append("Evening: ").append(ns(eveningStart)).append(" - ").append(ns(eveningEnd)).append("\n");

        if ("3".equals(shiftType)) {
            sb.append("Night: ").append(ns(nightStart)).append(" - ").append(ns(nightEnd)).append("\n");
        }

        sb.append("\nEmployees per shift: ").append(employeesPerShift == null ? "?" : employeesPerShift).append("\n");
        sb.append("Work days: ").append(workDays).append("\n");

        tvConfigSummary.setText(sb.toString());

        if (canSubmit) {
            tvSubmitStatus.setText("Availability submission is OPEN");
            btnOpenAvailability.setEnabled(true);
        } else {
            tvSubmitStatus.setText("Availability submission is CLOSED (waiting for manager)");
            btnOpenAvailability.setEnabled(false);
        }
    }

    private String ns(String s) { return s == null ? "" : s; }
}

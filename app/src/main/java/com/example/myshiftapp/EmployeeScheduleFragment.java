package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
    private Button btnOpenAvailability, btnBack, btnViewFullSchedule;

    public EmployeeScheduleFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_employee_schedule, container, false);

        tvConfigSummary = view.findViewById(R.id.tvConfigSummary);
        tvSubmitStatus = view.findViewById(R.id.tvSubmitStatus);

        btnOpenAvailability = view.findViewById(R.id.btnOpenAvailability);
        btnViewFullSchedule = view.findViewById(R.id.btnViewFullSchedule);
        btnBack = view.findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();

// Back (NAV)
        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

// Open availability grid
        btnOpenAvailability.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.shiftScheduleFragment);
        });

// Full schedule (Initially disabled until we know the ShiftSchedule has been published)
        btnViewFullSchedule.setEnabled(false);
        btnViewFullSchedule.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_employeeSchedule_to_fullSchedule)
        );

        loadShiftConfig(); // Displays shift description
        loadScheduleStatus(); // Determines whether full ShiftSchedule + open/closed status can be seen

        return view;
    }

    // =========================
// 1) settings/shift_config (Description of shifts)
// =========================
    private void loadShiftConfig() {
        db.collection("settings").document("shift_config")
                .get()
                .addOnSuccessListener(this::handleShiftConfig)
                .addOnFailureListener(e -> {
                    tvConfigSummary.setText("Failed to load shift settings.");
                    btnOpenAvailability.setEnabled(false);
                });
    }

    private void handleShiftConfig(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            tvConfigSummary.setText("No settings found. Ask manager to configure shifts.");
            btnOpenAvailability.setEnabled(false);
            return;
        }

        String shiftType = doc.getString("shiftType"); // "2"/"3"
        String workDays = doc.getString("workDays"); // SunThu/SunFri/SunSat

        String morningStart = doc.getString("morningStart");
        String morningEnd = doc.getString("morningEnd");
        String eveningStart = doc.getString("eveningStart");
        String eveningEnd = doc.getString("eveningEnd");
        String nightStart = doc.getString("nightStart");
        String nightEnd = doc.getString("nightEnd");

        Long employeesPerShift = doc.getLong("employeesPerShift");

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
        btnOpenAvailability.setEnabled(true); // The entry into the table is actually determined within the ShiftScheduleFragment by deadline+switch
    }

    private String ns(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    // =========================
// 2) scheduleConfig/currentWeek (Status + ShiftSchedule Advertising)
// =========================
    private void loadScheduleStatus() {
        db.collection("scheduleConfig").document("currentWeek")
                .get()
                .addOnSuccessListener(cfg -> {
                    Boolean submissionOpen = cfg.getBoolean("submissionOpen");
                    Long deadlineMillis = cfg.getLong("submissionDeadlineMillis");
                    Boolean scheduleBuilt = cfg.getBoolean("scheduleBuilt");

                    boolean isOpen = (submissionOpen != null && submissionOpen);

                    long now = System.currentTimeMillis();
                    boolean hasDeadline = (deadlineMillis != null && deadlineMillis > 0);
                    boolean beforeDeadline = !hasDeadline || now <= deadlineMillis;

                    if (isOpen && beforeDeadline) {
                        tvSubmitStatus.setText("Availability submission is OPEN");
                    } else {
                        tvSubmitStatus.setText("Availability submission is CLOSED");
                    }

// Only if the administrator published/built a ShiftSchedule
                    boolean canView = (scheduleBuilt != null && scheduleBuilt);
                    btnViewFullSchedule.setEnabled(canView);
                })
                .addOnFailureListener(e -> {
                    tvSubmitStatus.setText("Failed to load schedule status: " + e.getMessage());
                    btnViewFullSchedule.setEnabled(false);
                });
    }
}

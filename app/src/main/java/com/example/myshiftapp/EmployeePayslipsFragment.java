package com.example.myshiftapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployeePayslipsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvMonth, tvRate, tvApprovedCount, tvTotalHours, tvBase, tvExtras, tvGross;
    private Button btnGenerate, btnBack;

    private final SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    public EmployeePayslipsFragment() {
        super(R.layout.fragment_employee_payslips);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvMonth = view.findViewById(R.id.tvMonth);
        tvRate = view.findViewById(R.id.tvRate);
        tvApprovedCount = view.findViewById(R.id.tvApprovedCount);
        tvTotalHours = view.findViewById(R.id.tvTotalHours);
        tvBase = view.findViewById(R.id.tvBasePay);
        tvExtras = view.findViewById(R.id.tvExtrasPay);
        tvGross = view.findViewById(R.id.tvGross);

        btnGenerate = view.findViewById(R.id.btnGeneratePayslip);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        String monthKey = monthFmt.format(Calendar.getInstance().getTime());
        tvMonth.setText("Month: " + monthKey);

        btnGenerate.setOnClickListener(v -> generatePayslipForMonth(monthKey));

// Optional: show last saved payslip if exists
        loadSavedPayslipIfExists(monthKey);
    }

    private void loadSavedPayslipIfExists(String monthKey) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("payslips")
                .document(user.getUid())
                .collection("months")
                .document(monthKey)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    Double rate = doc.getDouble("hourlyRateSnapshot");
                    Long totalMinutes = doc.getLong("totalMinutesApproved");
                    Double basePay = doc.getDouble("basePay");
                    Double extrasPay = doc.getDouble("extrasPay");
                    Double gross = doc.getDouble("grossPay");

                    if (rate != null) tvRate.setText("Hourly rate: " + rate);

                    if (totalMinutes != null) {
                        double hours = totalMinutes / 60.0;
                        tvTotalHours.setText(String.format(Locale.getDefault(), "Total hours: %.2f", hours));
                    }

                    if (basePay != null) tvBase.setText(String.format(Locale.getDefault(), "Base pay: %.2f", basePay));
                    if (extrasPay != null) tvExtras.setText(String.format(Locale.getDefault(), "Extras: %.2f", extrasPay));
                    if (gross != null) tvGross.setText(String.format(Locale.getDefault(), "Gross pay: %.2f", gross));

                    tvApprovedCount.setText("Loaded saved payslip ✅");
                });
    }

    private void generatePayslipForMonth(String monthKey) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

// 1) Read hourlyRate from users/{uid}
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(userDoc -> {

                    Double hourlyRate = userDoc.getDouble("hourlyRate");

// If stored as Long/Int in Firestore, this can be null in getDouble()
                    if (hourlyRate == null && userDoc.getLong("hourlyRate") != null) {
                        hourlyRate = userDoc.getLong("hourlyRate").doubleValue();
                    }

                    if (hourlyRate == null) {
                        hourlyRate = 0.0;
                        Toast.makeText(requireContext(),
                                "hourlyRate is missing in users/" + user.getUid() + " (set it in Firestore)",
                                Toast.LENGTH_LONG).show();
                    }

                    tvRate.setText("Hourly rate: " + hourlyRate);

// 2) Load APPROVED attendance records for this month
                    loadApprovedAttendanceAndCompute(user.getUid(), monthKey, hourlyRate);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load user rate: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadApprovedAttendanceAndCompute(String uid, String monthKey, double hourlyRate) {
        db.collection("attendance")
                .document(uid)
                .collection("records")
                .whereEqualTo("status", "APPROVED")
                .whereEqualTo("monthKey", monthKey)
                .orderBy("startTimeMillis", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    ArrayList<AttendanceRecord> approved = new ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        String id = d.getId();
                        String shiftType = d.getString("shiftType");
                        Long start = d.getLong("startTimeMillis");
                        Long end = d.getLong("endTimeMillis");
                        Long mins = d.getLong("durationMinutes");
                        String status = d.getString("status");

                        if (start == null) start = 0L;
                        if (end == null) end = 0L;
                        if (mins == null) mins = 0L;
                        if (status == null) status = "APPROVED";

                        approved.add(new AttendanceRecord(id, shiftType, start, end, mins, status));
                    }

                    tvApprovedCount.setText("Approved records: " + approved.size());

                    PayslipCalculator.Result r = PayslipCalculator.calculate(approved, hourlyRate);

                    double hours = r.totalMinutes / 60.0;
                    tvTotalHours.setText(String.format(Locale.getDefault(), "Total hours: %.2f", hours));
                    tvBase.setText(String.format(Locale.getDefault(), "Base pay: %.2f", r.basePay));
                    tvExtras.setText(String.format(Locale.getDefault(), "Extras: %.2f", r.extrasPay));
                    tvGross.setText(String.format(Locale.getDefault(), "Gross pay: %.2f", r.grossPay));

                    savePayslipSnapshot(uid, monthKey, hourlyRate, r);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Load attendance failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void savePayslipSnapshot(String uid, String monthKey, double hourlyRate, PayslipCalculator.Result r) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("monthKey", monthKey);
        doc.put("hourlyRateSnapshot", hourlyRate);
        doc.put("totalMinutesApproved", r.totalMinutes);
        doc.put("basePay", r.basePay);
        doc.put("extrasPay", r.extrasPay);
        doc.put("grossPay", r.grossPay);
        doc.put("createdAt", FieldValue.serverTimestamp());

        db.collection("payslips")
                .document(uid)
                .collection("months")
                .document(monthKey)
                .set(doc)
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), "Payslip saved ", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Payslip save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
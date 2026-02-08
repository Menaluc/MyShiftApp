package com.example.myshiftapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployeeAttendanceFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvStatus;
    private Spinner spShiftType;
    private Button btnClockIn, btnClockOut, btnBack;

    private RecyclerView rv;
    private AttendanceRecordAdapter adapter;
    private final ArrayList<AttendanceRecord> records = new ArrayList<>();

    // Local state: current open shift (in-memory only for now)
    private Long openStartMillis = null;
    private String openShiftType = null;

    private final SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    public EmployeeAttendanceFragment() {
        super(R.layout.fragment_employee_attendance);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvStatus = view.findViewById(R.id.tvStatus);
        spShiftType = view.findViewById(R.id.spShiftType);
        btnClockIn = view.findViewById(R.id.btnClockIn);
        btnClockOut = view.findViewById(R.id.btnClockOut);
        btnBack = view.findViewById(R.id.btnBack);

        rv = view.findViewById(R.id.rvRecords);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AttendanceRecordAdapter(records);
        rv.setAdapter(adapter);

// Spinner setup
        ArrayAdapter<CharSequence> spAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.shift_types,
                android.R.layout.simple_spinner_item
        );
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spShiftType.setAdapter(spAdapter);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        btnClockIn.setOnClickListener(v -> onClockIn());
        btnClockOut.setOnClickListener(v -> onClockOut());

        loadThisMonthRecords();
        refreshUiState();
    }

    private void onClockIn() {
        if (openStartMillis != null) {
            Toast.makeText(requireContext(), "Already clocked in. Please clock out first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String shiftType = String.valueOf(spShiftType.getSelectedItem());
        if (TextUtils.isEmpty(shiftType)) shiftType = "Morning";

        openStartMillis = System.currentTimeMillis();
        openShiftType = shiftType;

        Toast.makeText(requireContext(), "Clock In saved locally ✅", Toast.LENGTH_SHORT).show();
        refreshUiState();
    }

    private void onClockOut() {
        if (openStartMillis == null || openShiftType == null) {
            Toast.makeText(requireContext(), "You are not clocked in.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        long endMillis = System.currentTimeMillis();
        long startMillis = openStartMillis;

        if (endMillis <= startMillis) {
            Toast.makeText(requireContext(), "Clock out time is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        long durationMinutes = (endMillis - startMillis) / (60_000);

// Build monthKey like "2026-02"
        String monthKey = monthFmt.format(Calendar.getInstance().getTime());

// Firestore doc: attendance/{uid}/records/{autoId}
        Map<String, Object> data = new HashMap<>();
        data.put("shiftType", openShiftType);
        data.put("startTimeMillis", startMillis);
        data.put("endTimeMillis", endMillis);
        data.put("durationMinutes", durationMinutes);
        data.put("monthKey", monthKey);
        data.put("status", "PENDING");
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("attendance")
                .document(user.getUid())
                .collection("records")
                .add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(requireContext(), "Attendance saved ✅ (PENDING)", Toast.LENGTH_SHORT).show();

// Reset open shift
                    openStartMillis = null;
                    openShiftType = null;

                    refreshUiState();
                    loadThisMonthRecords();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void refreshUiState() {
        if (openStartMillis == null) {
            tvStatus.setText("Not clocked in");
            btnClockIn.setEnabled(true);
            btnClockOut.setEnabled(false);
        } else {
            tvStatus.setText("Clocked in (" + openShiftType + ") ...");
            btnClockIn.setEnabled(false);
            btnClockOut.setEnabled(true);
        }
    }

    private void loadThisMonthRecords() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String monthKey = monthFmt.format(Calendar.getInstance().getTime());

        db.collection("attendance")
                .document(user.getUid())
                .collection("records")
                .whereEqualTo("monthKey", monthKey)
                .orderBy("startTimeMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    records.clear();
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

                        records.add(new AttendanceRecord(id, shiftType, start, end, mins, status));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerAttendanceApprovalsFrg extends Fragment {

    private FirebaseFirestore db;

    private TextView tvInfo;
    private RecyclerView rv;

    private final List<ApprovalItem> items = new ArrayList<>();
    private ManagerAttendanceApprovalsAdapter adapter;

    public ManagerAttendanceApprovalsFrg() {
        super(R.layout.fragment_manager_attendance_approvals_frg);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        Button btnBack = view.findViewById(R.id.btnBack);
        tvInfo = view.findViewById(R.id.tvInfo);
        rv = view.findViewById(R.id.rvApprovals);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ManagerAttendanceApprovalsAdapter(items, this::approveItem);
        rv.setAdapter(adapter);

        loadPending();
    }

    private void loadPending() {
        tvInfo.setText("Loading pending attendance...");

        items.clear();
        adapter.notifyDataSetChanged();

// Scan attendance/* then records where status=PENDING
        db.collection("attendance")
                .get()
                .addOnSuccessListener(usersSnap -> {

                    if (usersSnap.isEmpty()) {
                        tvInfo.setText("No attendance found.");
                        return;
                    }

                    final int[] totalUsers = {usersSnap.size()};
                    final int[] processed = {0};
                    final int[] pendingCount = {0};

                    usersSnap.getDocuments().forEach(userDoc -> {
                        String uid = userDoc.getId();

                        db.collection("attendance")
                                .document(uid)
                                .collection("records")
                                .whereEqualTo("status", "PENDING")
                                .get()
                                .addOnSuccessListener(recSnap -> {

                                    for (QueryDocumentSnapshot r : recSnap) {
                                        String recordId = r.getId();

                                        String shiftType = r.getString("shiftType");
                                        Long start = r.getLong("startTimeMillis");
                                        Long end = r.getLong("endTimeMillis");
                                        Long mins = r.getLong("durationMinutes");
                                        String status = r.getString("status");

                                        if (shiftType == null) shiftType = "Unknown";
                                        if (start == null) start = 0L;
                                        if (end == null) end = 0L;
                                        if (mins == null) mins = 0L;
                                        if (status == null) status = "PENDING";

// Try to fetch employee full name from users/{uid}.fullName
                                        fetchEmployeeNameAndAdd(uid, recordId, shiftType, start, end, mins, status);

                                        pendingCount[0]++;
                                    }

                                    processed[0]++;
                                    if (processed[0] == totalUsers[0]) {
                                        tvInfo.setText("Pending records: " + items.size());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processed[0]++;
                                    if (processed[0] == totalUsers[0]) {
                                        tvInfo.setText("Done (with errors). Pending loaded: " + items.size());
                                    }
                                });
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void fetchEmployeeNameAndAdd(String uid,
                                         String recordId,
                                         String shiftType,
                                         long start,
                                         long end,
                                         long mins,
                                         String status) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userSnap -> {
                    String fullName = userSnap.getString("fullName");
                    if (fullName == null || fullName.trim().isEmpty()) fullName = uid;

                    items.add(new ApprovalItem(uid, recordId, fullName, shiftType, start, end, mins, status));
                    adapter.notifyDataSetChanged();
                    tvInfo.setText("Pending records: " + items.size());
                })
                .addOnFailureListener(e -> {
// fallback to uid
                    items.add(new ApprovalItem(uid, recordId, uid, shiftType, start, end, mins, status));
                    adapter.notifyDataSetChanged();
                    tvInfo.setText("Pending records: " + items.size());
                });
    }

    private void approveItem(ApprovalItem item) {
        db.collection("attendance")
                .document(item.uid)
                .collection("records")
                .document(item.recordId)
                .update("status", "APPROVED")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Approved ", Toast.LENGTH_SHORT).show();

// Remove by uid+recordId (safe)
                    for (int i = 0; i < items.size(); i++) {
                        ApprovalItem x = items.get(i);
                        if (x.uid.equals(item.uid) && x.recordId.equals(item.recordId)) {
                            items.remove(i);
                            adapter.notifyItemRemoved(i);
                            break;
                        }
                    }

                    tvInfo.setText("Pending records: " + items.size());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Approve failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
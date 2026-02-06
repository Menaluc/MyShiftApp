package com.example.myshiftapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManagerEmployeesFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView rv;

    public ManagerEmployeesFragment() {
        super(R.layout.fragment_manager_employees);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rv = view.findViewById(R.id.rvEmployees);
        Button btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadEmployees();
    }

    private void loadEmployees() {
        db.collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<EmployeeStatus> list = new ArrayList<>();

                    for (var doc : snapshot.getDocuments()) {
                        String role = doc.getString("role");
                        if (!"employee".equalsIgnoreCase(role)) continue;

                        String uid = doc.getId();
                        String fullName = doc.getString("fullName");
                        if (TextUtils.isEmpty(fullName)) fullName = uid;

                        Boolean submitted = doc.getBoolean("submitted");
                        Timestamp submittedAt = doc.getTimestamp("submittedAt");

                        String submittedAtText = null;
                        if (submittedAt != null) {
                            submittedAtText = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(submittedAt.toDate());
                        }

                        list.add(new EmployeeStatus(
                                uid,
                                fullName,
                                submitted != null && submitted,
                                submittedAtText
                        ));
                    }

                    rv.setAdapter(new EmployeeStatusAdapter(list));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
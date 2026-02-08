package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ManagerHomeFragment extends Fragment {

    private static final String ARG_FULL_NAME = "arg_full_name";

    public static ManagerHomeFragment newInstance(String fullName) {
        ManagerHomeFragment f = new ManagerHomeFragment();
        Bundle b = new Bundle();
        b.putString(ARG_FULL_NAME, fullName);
        f.setArguments(b);
        return f;
    }

    public ManagerHomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manager_home, container, false);

// --- Welcome text ---
        String fullName = (getArguments() == null)
                ? ""
                : getArguments().getString(ARG_FULL_NAME, "");

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome " + fullName);

// --- Menu button: open MAIN drawer (from MainActivity) ---
        ImageButton btnMenu = view.findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
// Ask MainActivity to open the single shared drawer
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openRightDrawer();
            }
        });

// --- Debug: seed demo data ---
        Button btnSeedDemo = view.findViewById(R.id.btnSeedDemo);
        if (btnSeedDemo != null) {
            btnSeedDemo.setOnClickListener(v -> seedDemoData());
        }

        return view;
    }

    // ============================
// Demo Seeder (Firestore only)
// ============================
    private void seedDemoData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String weekId = "demo-week"; // Fixed demo week for now

        String[] names = {
                "Alice", "Bob", "Carol", "David", "Emma",
                "Frank", "Grace", "Henry", "Irene", "Jack"
        };

        WriteBatch batch = db.batch();

        for (int i = 0; i < names.length; i++) {
            String docId = String.format("demo_%02d", i + 1); // demo_01..demo_10

// 1) users/demo_XX
            Map<String, Object> userDoc = new HashMap<>();
            userDoc.put("fullName", names[i]);
            userDoc.put("role", "employee");
            userDoc.put("isDemo", true);

            batch.set(db.collection("users").document(docId), userDoc);

// 2) users/demo_XX/availabilityByWeek/demo-week
            Map<String, Object> weekDoc = new HashMap<>();
            weekDoc.put("submitted", true);
            weekDoc.put("submittedAt", Timestamp.now());
            weekDoc.put("exempt", false);
            weekDoc.put("exemptReason", "");

            Map<String, Object> availability = buildDemoAvailabilityForIndex(i);
            weekDoc.put("availability", availability);

            batch.set(
                    db.collection("users")
                            .document(docId)
                            .collection("availabilityByWeek")
                            .document(weekId),
                    weekDoc
            );
        }

        batch.commit()
                .addOnSuccessListener(unused ->
                        Toast.makeText(requireContext(), "Demo data seeded ✅", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Seed failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private Map<String, Object> buildDemoAvailabilityForIndex(int i) {
        Map<String, Object> m = new HashMap<>();

        switch (i) {
            case 0:
                putTrue(m, "Sun_Morning", "Sun_Evening", "Mon_Morning", "Tue_Morning", "Wed_Evening", "Thu_Morning");
                break;
            case 1:
                putTrue(m, "Sun_Evening", "Mon_Evening", "Tue_Evening", "Wed_Evening", "Thu_Evening");
                break;
            case 2:
                putTrue(m, "Mon_Morning", "Tue_Morning", "Wed_Morning");
                break;
            case 3:
                putTrue(m, "Sun_Morning", "Mon_Morning", "Mon_Evening", "Tue_Evening", "Thu_Morning");
                break;
            case 4:
                putTrue(m, "Sun_Morning", "Sun_Evening", "Tue_Evening", "Wed_Morning", "Thu_Evening");
                break;
            case 5:
                putTrue(m, "Sun_Morning", "Mon_Morning", "Tue_Morning");
                break;
            case 6:
                putTrue(m, "Mon_Evening", "Tue_Evening", "Wed_Evening", "Thu_Morning", "Thu_Evening");
                break;
            case 7:
                putTrue(m, "Sun_Morning", "Sun_Evening", "Mon_Morning", "Mon_Evening", "Tue_Morning", "Wed_Morning", "Thu_Morning");
                break;
            case 8:
                putTrue(m, "Wed_Evening", "Thu_Evening");
                break;
            case 9:
                putTrue(m, "Sun_Morning", "Mon_Evening", "Tue_Morning", "Wed_Evening", "Thu_Morning");
                break;
        }

        return m;
    }

    private void putTrue(Map<String, Object> m, String... keys) {
        for (String k : keys) m.put(k, true);
    }
}
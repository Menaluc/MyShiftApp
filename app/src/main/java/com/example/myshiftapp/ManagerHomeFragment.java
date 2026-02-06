
package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_manager_home, container, false);

        // Welcome text
        String fullName = (getArguments() == null) ? "" : getArguments().getString(ARG_FULL_NAME, "");
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome " + fullName);

        // Drawer + menu
        DrawerLayout drawerLayout = view.findViewById(R.id.managerDrawer);
        NavigationView navView = view.findViewById(R.id.managerNavView);
        ImageButton btnMenu = view.findViewById(R.id.btnMenu);

        // Open/close right drawer
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        //Seed Demo button
        Button btnSeedDemo = view.findViewById(R.id.btnSeedDemo);
        if (btnSeedDemo != null) {
            btnSeedDemo.setOnClickListener(v -> seedDemoData());
        }

        // Menu clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_settings) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_managerHome_to_managerSettings);

            } else if (id == R.id.nav_employees) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_managerHome_to_managerEmployees);

            } else if (id == R.id.nav_build_schedule) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_managerHome_to_managerBuildSchedule);

            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                NavHostFragment.findNavController(this)
                        .navigate(R.id.loginFragment);
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        return view;
    }

    // ============================
    //Demo Seeder (Firestore only)
    // ============================

    private void seedDemoData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String weekId = "demo-week"; // fixed demo week for now

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

            batch.set(
                    db.collection("users").document(docId),
                    userDoc
            );

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

        // Days: Sun-Thu, Shifts: Morning/Evening
        switch (i) {
            case 0: // Alice - very available
                putTrue(m, "Sun_Morning", "Sun_Evening", "Mon_Morning", "Tue_Morning", "Wed_Evening", "Thu_Morning");
                break;

            case 1: // Bob - evenings
                putTrue(m, "Sun_Evening", "Mon_Evening", "Tue_Evening", "Wed_Evening", "Thu_Evening");
                break;

            case 2: // Carol - limited mornings
                putTrue(m, "Mon_Morning", "Tue_Morning", "Wed_Morning");
                break;

            case 3: // David
                putTrue(m, "Sun_Morning", "Mon_Morning", "Mon_Evening", "Tue_Evening", "Thu_Morning");
                break;

            case 4: // Emma
                putTrue(m, "Sun_Morning", "Sun_Evening", "Tue_Evening", "Wed_Morning", "Thu_Evening");
                break;

            case 5: // Frank - early week mornings
                putTrue(m, "Sun_Morning", "Mon_Morning", "Tue_Morning");
                break;

            case 6: // Grace - evenings + Thu
                putTrue(m, "Mon_Evening", "Tue_Evening", "Wed_Evening", "Thu_Morning", "Thu_Evening");
                break;

            case 7: // Henry - almost free
                putTrue(m, "Sun_Morning", "Sun_Evening", "Mon_Morning", "Mon_Evening", "Tue_Morning", "Wed_Morning", "Thu_Morning");
                break;

            case 8: // Irene - edge case
                putTrue(m, "Wed_Evening", "Thu_Evening");
                break;

            case 9: // Jack - balanced
                putTrue(m, "Sun_Morning", "Mon_Evening", "Tue_Morning", "Wed_Evening", "Thu_Morning");
                break;
        }

        return m;
    }

    private void putTrue(Map<String, Object> m, String... keys) {
        for (String k : keys) m.put(k, true);
    }
}
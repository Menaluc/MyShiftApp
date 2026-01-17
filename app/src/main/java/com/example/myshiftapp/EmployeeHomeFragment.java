package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class EmployeeHomeFragment extends Fragment {

    private static final String ARG_FULL_NAME = "arg_full_name";

    public static EmployeeHomeFragment newInstance(String fullName) {
        EmployeeHomeFragment f = new EmployeeHomeFragment();
        Bundle b = new Bundle();
        b.putString(ARG_FULL_NAME, fullName);
        f.setArguments(b);
        return f;
    }

    public EmployeeHomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_employee_home, container, false);

        String fullName = (getArguments() == null) ? "" : getArguments().getString(ARG_FULL_NAME, "");

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome " + fullName);

        DrawerLayout drawerLayout = view.findViewById(R.id.employeeDrawer);
        NavigationView navView = view.findViewById(R.id.employeeNavView);
        ImageButton btnMenu = view.findViewById(R.id.btnMenu);

        // Open/close right drawer
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        // Menu actions
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_employee_profile) {
                Toast.makeText(requireContext(), "Employee Profile", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_schedule) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new ShiftScheduleFragment())
                        .addToBackStack(null)
                        .commit();

            } else if (id == R.id.nav_attendance) {
                Toast.makeText(requireContext(), "Report Attendance", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_monthly_details) {
                Toast.makeText(requireContext(), "Monthly Details", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_payslips) {
                Toast.makeText(requireContext(), "Payslips", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();

                requireActivity().getSupportFragmentManager().popBackStack(null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new LoginFragment())
                        .commit();

                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        return view;
    }
}

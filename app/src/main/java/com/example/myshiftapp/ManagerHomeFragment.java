package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

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

        // Menu clicks
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_settings) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.contentManager, new ManagerSettingsFragment())
                        .addToBackStack(null)
                        .commit();

            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.contentManager, new LoginFragment())
                        .commit();
            }

            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        return view;
    }
}

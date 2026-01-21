package com.example.myshiftapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

public class EmployeeHomeFragment extends Fragment {

    private static final String ARG_FULL_NAME_OLD = "arg_full_name";
    private static final String ARG_FULL_NAME_NEW = "fullName";

    public EmployeeHomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_employee_home, container, false);

        // ---- fullName: args חדש -> ישן -> UserStorage ----
        String fullName = "";
        Bundle args = getArguments();
        if (args != null) {
            fullName = args.getString(ARG_FULL_NAME_NEW, "");
            if (fullName == null || fullName.isEmpty()) {
                fullName = args.getString(ARG_FULL_NAME_OLD, "");
            }
        }
        if (fullName == null) fullName = "";
        if (fullName.isEmpty()) {
            fullName = UserStorage.getFullName(requireContext());
            if (fullName == null) fullName = "";
        }

        // ---- find views safely ----
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        ImageButton btnMenu = view.findViewById(R.id.btnMenu);

        Log.d("EMP_HOME", "layout=fragment_employee_home tvWelcome="
                + (tvWelcome != null) + " btnMenu=" + (btnMenu != null));

        if (tvWelcome != null) {
            tvWelcome.setText("Welcome " + fullName);
        } else {
            Log.e("EMP_HOME", "tvWelcome is NULL -> check fragment_employee_home.xml has @+id/tvWelcome");
        }

        // ✅ Open the MAIN drawer (activity_main.xml drawerLayout)
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawerLayout);
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            });
        } else {
            Log.e("EMP_HOME", "btnMenu is NULL -> check fragment_employee_home.xml has @+id/btnMenu");
        }

        return view;
    }
}

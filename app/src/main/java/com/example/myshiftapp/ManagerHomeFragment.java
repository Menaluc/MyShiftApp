package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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

        // Read full name from arguments
        String fullName = (getArguments() == null)
                ? ""
                : getArguments().getString(ARG_FULL_NAME, "");

        // Views from your XML
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        Button btnLogout = view.findViewById(R.id.btnManagerLogout);

        // Set welcome text
        tvWelcome.setText("Welcome " + fullName);

        // Logout (Firebase)
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .commit();
        });

        return view;
    }
}

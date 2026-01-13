package com.example.myshiftapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class LoginFragment extends Fragment {

    public LoginFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Keep consistent variable names
        EditText enterUsername = view.findViewById(R.id.Username);
        EditText enterPassword = view.findViewById(R.id.Password);
        TextView tvForgotPassword = view.findViewById(R.id.ForgotPassword);
        Button btnLogin = view.findViewById(R.id.Login);

        btnLogin.setOnClickListener(v -> {
            String userId = safeText(enterUsername);
            String password = safeText(enterPassword);

            // ID: exactly 9 digits
            if (!userId.matches("^\\d{9}$")) {
                Toast.makeText(requireContext(), "ID must contain exactly 9 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            // Password basic
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check user exists
            if (!UserStorage.isUserKnown(requireContext(), userId)) {
                Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check password matches stored
            if (!UserStorage.checkPassword(requireContext(), userId, password)) {
                Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                return;
            }

            // First login -> force change password
            if (UserStorage.isFirstLogin(requireContext(), userId)) {
                openChangePassword(userId);
                return;
            }

            // Not first login -> go to home by role
            navigateToHomeByRole(userId);
        });

        tvForgotPassword.setOnClickListener(v -> {
            String userId = safeText(enterUsername);

            // optional: validate ID first
            if (!userId.matches("^\\d{9}$")) {
                Toast.makeText(requireContext(), "Enter valid 9-digit ID first", Toast.LENGTH_SHORT).show();
                return;
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, ChangePasswordFragment.newInstance(userId))
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void openChangePassword(String userId) {
        ChangePasswordFragment fragment = ChangePasswordFragment.newInstance(userId);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate user to correct home screen based on role
    private void navigateToHomeByRole(String userId) {
        String role = UserStorage.getRole(requireContext(), userId);

        Fragment next;
        if ("manager".equals(role)) {
            next = new ManagerHomeFragment();
        } else {
            next = new EmployeeHomeFragment();
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, next)
                .commit();
    }

    // Safely get trimmed text from EditText
    private String safeText(EditText enter) {
        if (enter == null || enter.getText() == null) return "";
        return enter.getText().toString().trim();
    }
}

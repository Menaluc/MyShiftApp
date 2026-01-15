package com.example.myshiftapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class ChangePasswordFragment extends Fragment {

    public ChangePasswordFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        EditText etNewPass = view.findViewById(R.id.newPasswordEditText);
        EditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        Button btnSave = view.findViewById(R.id.btnSavePassword);

        btnSave.setOnClickListener(v -> {
            String newPass = safeText(etNewPass);
            String confirmPass = safeText(etConfirm);

            // Basic validations
            if (TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(requireContext(), "Please fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Password rules (same as before): max 10, at least 1 English letter + 1 digit, no Hebrew
            String error = validatePassword(newPass);
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO (Later): Implement REAL password change for a LOGGED-IN user using Firebase:
            // FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            // user.updatePassword(newPass) ...
            //
            // For "Forgot password" we already use Firebase reset email in LoginFragment.

            Toast.makeText(requireContext(),
                    "Change Password screen is ready. (TODO: connect to Firebase updatePassword)",
                    Toast.LENGTH_LONG).show();

            // Optional: navigate back
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }

    // Safely get trimmed text from EditText
    private String safeText(EditText enter) {
        if (enter == null || enter.getText() == null) return "";
        return enter.getText().toString().trim();
    }

    // Returns null if valid, otherwise returns an error message
    private String validatePassword(String password) {
        if (password.length() > 10) return "Password must be at most 10 characters";

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            // Block Hebrew letters (Unicode range)
            if (c >= '\u0590' && c <= '\u05FF') {
                return "Password cannot contain Hebrew letters";
            }

            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) hasLetter = true;
            if (c >= '0' && c <= '9') hasDigit = true;
        }

        if (!hasLetter) return "Password must include at least 1 English letter";
        if (!hasDigit) return "Password must include at least 1 number";

        return null;
    }
}

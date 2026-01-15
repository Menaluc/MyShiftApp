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

    private static final String ARG_EMAIL = "arg_email";

    public ChangePasswordFragment() {}

    public static ChangePasswordFragment newInstance(String email) {
        ChangePasswordFragment f = new ChangePasswordFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EMAIL, email);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        EditText etNewPass = view.findViewById(R.id.newPasswordEditText);
        EditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        Button btnSave = view.findViewById(R.id.btnSavePassword);

        String email = (getArguments() == null) ? null : getArguments().getString(ARG_EMAIL);

        btnSave.setOnClickListener(v -> {
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(), "Missing email", Toast.LENGTH_SHORT).show();
                return;
            }

            String newPass = safeText(etNewPass);
            String confirmPass = safeText(etConfirm);

            if (TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(requireContext(), "Please fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            String error = validatePassword(newPass);
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                return;
            }

            // Save password + mark first login done (local until Firebase)
            UserStorage.setPasswordAndMarkFirstLoginDone(requireContext(), email, newPass);

            Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();

            Fragment next = "manager".equals(UserStorage.getRole(requireContext(), email))
                    ? new ManagerHomeFragment()
                    : new EmployeeHomeFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, next)
                    .commit();
        });

        return view;
    }

    private String safeText(EditText enter) {
        if (enter == null || enter.getText() == null) return "";
        return enter.getText().toString().trim();
    }

    private String validatePassword(String password) {
        if (password.length() > 10) return "Password must be at most 10 characters";

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            // Block Hebrew letters
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

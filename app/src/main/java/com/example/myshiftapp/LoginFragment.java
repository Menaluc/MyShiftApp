package com.example.myshiftapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText enterEmail; //  enterEmail
    private EditText enterPassword;
    private TextView tvForgotPassword;
    private Button btnLogin;

    public LoginFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

// Views
        enterEmail = view.findViewById(R.id.Email);
        enterPassword = view.findViewById(R.id.Password);
        tvForgotPassword = view.findViewById(R.id.ForgotPassword);
        btnLogin = view.findViewById(R.id.Login);

// Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

// Login
        btnLogin.setOnClickListener(v -> {
            String email = safeText(enterEmail);
            String password = safeText(enterPassword);

// Basic validation
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }

// Sign in Firebase Auth
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(requireContext(), "Login failed (no user)", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = user.getUid();
                        loadUserAndNavigate(uid);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

// Forgot password (כרגע רק ניווט למסך שינוי סיסמה אצלכן)
        tvForgotPassword.setOnClickListener(v -> {
            String email = safeText(enterEmail);

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Enter a valid email first", Toast.LENGTH_SHORT).show();
                return;
            }

// אם עדיין משתמשים במסך ChangePasswordFragment פנימי:
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, ChangePasswordFragment.newInstance(email))
                    .addToBackStack(null)
                    .commit();

// בהמשך (מומלץ) נעשה Firebase password reset:
// mAuth.sendPasswordResetEmail(email) ...
        });

        return view;
    }

    private void loadUserAndNavigate(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(requireContext(), "User data not found in Firestore", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String role = doc.getString("role");
                    String fullName = doc.getString("fullName");

                    if (role == null) role = "employee";
                    if (fullName == null) fullName = "";

                    navigateToHomeByRole(role, fullName);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToHomeByRole(String role, String fullName) {
        Fragment next = "manager".equalsIgnoreCase(role)
                ? ManagerHomeFragment.newInstance(fullName)
                : EmployeeHomeFragment.newInstance(fullName);

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
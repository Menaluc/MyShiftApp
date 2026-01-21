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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText enterEmail;
    private EditText enterPassword;
    private TextView tvForgotPassword;
    private Button btnLogin;

    public LoginFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        enterEmail = view.findViewById(R.id.Email);
        enterPassword = view.findViewById(R.id.Password);
        tvForgotPassword = view.findViewById(R.id.ForgotPassword);
        btnLogin = view.findViewById(R.id.Login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> doLogin());

        tvForgotPassword.setOnClickListener(v -> {
            String email = safeText(enterEmail);

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Enter a valid email first", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(requireContext(),
                                    "Password reset email sent. Check your inbox.",
                                    Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ✅ AUTO-LOGIN: Firebase הוא האמת (לא תלוי UserStorage)
        FirebaseUser current = mAuth.getCurrentUser();
        if (current != null) {
            // אם יש role שמור - נשתמש בו מהר, אחרת נטען מ-Firestore
            String role = UserStorage.getRole(requireContext());
            String fullName = UserStorage.getFullName(requireContext());

            if (!TextUtils.isEmpty(role)) {
                navigateToHomeByRole(role, fullName);
            } else {
                loadUserAndNavigate(current.getUid());
            }
        }
    }

    private void doLogin() {
        String email = safeText(enterEmail);
        String password = safeText(enterPassword);

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(requireContext(), "Login failed (no user)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    loadUserAndNavigate(user.getUid());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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
                    String email = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getEmail() : "";

                    if (role == null) role = "employee";
                    if (fullName == null) fullName = "";
                    if (email == null) email = "";

                    UserStorage.saveCurrentUser(requireContext(), uid, email, fullName, role);

                    navigateToHomeByRole(role, fullName);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void navigateToHomeByRole(String role, String fullName) {
        NavController navController = NavHostFragment.findNavController(this);

        int destId = "manager".equalsIgnoreCase(role)
                ? R.id.managerHomeFragment
                : R.id.employeeHomeFragment;

        Bundle args = new Bundle();
        args.putString("fullName", fullName);

        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build();

        navController.navigate(destId, args, navOptions);
    }

    private String safeText(EditText enter) {
        if (enter == null || enter.getText() == null) return "";
        return enter.getText().toString().trim();
    }
}

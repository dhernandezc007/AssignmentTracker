package com.example.assignmenttracker;


import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assignmenttracker.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        binding.tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Password reset coming soon", Toast.LENGTH_SHORT).show());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        // Clear old errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;

        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            valid = false;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            binding.tilPassword.setError("At least 6 characters");
            valid = false;
        }

        if (!valid) return;

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        setLoading(true);

        // TODO: replace with real authentication (Firebase Auth, your API, etc.)
        // Example with Firebase:
        // FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        //     .addOnCompleteListener(task -> {
        //         setLoading(false);
        //         if (task.isSuccessful()) goToMain();
        //         else Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
        //     });

        binding.getRoot().postDelayed(() -> {
            setLoading(false);
            goToMain();
        }, 1000); // fake delay for now
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
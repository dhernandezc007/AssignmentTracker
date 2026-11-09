package com.example.assignmenttracker;


import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.assignmenttracker.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.apply(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        binding.tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, R.string.toast_password_reset_soon, Toast.LENGTH_SHORT).show());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        // Clear old errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;

        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
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
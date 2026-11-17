package com.example.assignmenttracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.assignmenttracker.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // SECURITY: placeholder, not authentication. See SEC-0-1.
        // Off the launch path; release builds refuse to open it at all.
        if (!BuildConfig.AUTH_ENABLED) {
            finish();
            return;
        }
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.apply(binding.getRoot());

        binding.btnSignUp.setOnClickListener(v -> attemptSignUp());
        binding.tvLogin.setOnClickListener(v -> finish()); // back to login
    }

    private void attemptSignUp() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText().toString();

        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);

        boolean valid = true;

        if (name.isEmpty()) {
            binding.tilName.setError(getString(R.string.error_name_required));
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            valid = false;
        }
        if (!confirm.equals(password)) {
            binding.tilConfirm.setError(getString(R.string.error_passwords_mismatch));
            valid = false;
        }

        if (!valid) return;

        // SECURITY: placeholder, not authentication. See SEC-0-1.
        // TODO: create the account (Firebase Auth, your API, Room DB, etc.)
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }
}
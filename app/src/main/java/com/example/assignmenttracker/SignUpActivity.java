package com.example.assignmenttracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assignmenttracker.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
            binding.tilName.setError("Name is required");
            valid = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Enter a valid email");
            valid = false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("At least 6 characters");
            valid = false;
        }
        if (!confirm.equals(password)) {
            binding.tilConfirm.setError("Passwords don't match");
            valid = false;
        }

        if (!valid) return;

        // TODO: create the account (Firebase Auth, your API, Room DB, etc.)
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }
}
package com.mipim.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mipim.app.data.AuthRepository;
import com.mipim.app.data.Validators;
import com.mipim.app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final AuthRepository repository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sessão ativa: vai direto para a tela principal
        if (repository.isLoggedIn()) {
            goToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;
        if (!Validators.isValidEmail(email)) {
            binding.tilEmail.setError("Informe um e-mail válido");
            valid = false;
        }
        if (password.trim().isEmpty()) {
            binding.tilPassword.setError("Informe sua senha");
            valid = false;
        }
        if (!valid) return;

        setLoading(true);
        repository.login(email, password, new AuthRepository.Callback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                goToMain();
            }

            @Override
            public void onError(@NonNull Exception e) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        AuthRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }
}
package com.mipim.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mipim.app.data.AuthRepository;
import com.mipim.app.data.Validators;
import com.mipim.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private final AuthRepository repository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvGoLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText().toString();

        if (!validateFields(name, email, password, confirm)) return;

        setLoading(true);
        repository.register(name, email, password, new AuthRepository.Callback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
                        "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull Exception e) {
                setLoading(false);
                String msg = AuthRepository.friendlyError(e);
                if (AuthRepository.isEmailAlreadyUsed(e)) {
                    binding.tilEmail.setError(msg);
                } else {
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean validateFields(String name, String email, String password, String confirm) {
        boolean valid = true;
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);

        if (name.isEmpty()) {
            binding.tilName.setError("Informe seu nome");
            valid = false;
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError("Informe seu e-mail");
            valid = false;
        } else if (!Validators.isValidEmail(email)) {
            binding.tilEmail.setError("E-mail inválido");
            valid = false;
        }
        if (!Validators.isValidPassword(password)) {
            binding.tilPassword.setError("A senha deve ter no mínimo 6 caracteres");
            valid = false;
        }
        if (!confirm.equals(password)) {
            binding.tilConfirm.setError("As senhas não coincidem");
            valid = false;
        }
        return valid;
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }
}
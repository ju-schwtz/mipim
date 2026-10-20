package com.mipim.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mipim.app.R;
import com.mipim.app.data.AuthRepository;
import com.mipim.app.data.ColecaoRepository;
import com.mipim.app.data.PerfilRepository;
import com.mipim.app.databinding.ActivityPerfilBinding;

/** História 21: visualizar e editar os dados do perfil */
public class PerfilActivity extends AppCompatActivity {

    private static final int NOME_MIN = 2;
    private static final int NOME_MAX = 60;

    private ActivityPerfilBinding binding;
    private final PerfilRepository repository = new PerfilRepository();
    private boolean preenchido = false; // evita sobrescrever o que o usuário está digitando

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Navegacao.configurar(this, binding.navBar.bottomNav, R.id.nav_perfil);
        binding.btnSair.setOnClickListener(v -> sair());
        binding.btnSalvar.setOnClickListener(v -> salvar());
        binding.btnRedefinirSenha.setOnClickListener(v -> confirmarRedefinicao());
    }

    @Override
    protected void onStart() {
        super.onStart();

        repository.observar(this, new PerfilRepository.PerfilListener() {
            @Override
            public void onChange(String nome, String email) {
                binding.etEmail.setText(email);
                binding.tvEmailPerfil.setText(email);
                binding.tvNomePerfil.setText(nome != null && !nome.trim().isEmpty()
                        ? nome.trim() : "Leitor");
                if (!preenchido) {
                    binding.etNome.setText(nome);
                    preenchido = true;
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(PerfilActivity.this,
                        ColecaoRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sair() {
        new AuthRepository().logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void salvar() {
        String nome = binding.etNome.getText() == null
                ? "" : binding.etNome.getText().toString().trim();

        binding.tilNome.setError(null);
        if (nome.length() < NOME_MIN) {
            binding.tilNome.setError("Informe seu nome (mínimo " + NOME_MIN + " letras)");
            return;
        }
        if (nome.length() > NOME_MAX) {
            binding.tilNome.setError("Use no máximo " + NOME_MAX + " caracteres");
            return;
        }

        binding.btnSalvar.setEnabled(false);
        repository.atualizarNome(nome, new ColecaoRepository.Callback() {
            @Override
            public void onSuccess() {
                if (isDestroyed()) return;
                binding.btnSalvar.setEnabled(true);
                Toast.makeText(PerfilActivity.this,
                        "Perfil atualizado!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (isDestroyed()) return;
                binding.btnSalvar.setEnabled(true);
                Toast.makeText(PerfilActivity.this,
                        ColecaoRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmarRedefinicao() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Redefinir senha")
                .setMessage("Enviaremos um link para o seu e-mail cadastrado. Deseja continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar", (dialog, which) ->
                        repository.enviarRedefinicaoSenha(new ColecaoRepository.Callback() {
                            @Override
                            public void onSuccess() {
                                if (isDestroyed()) return;
                                Toast.makeText(PerfilActivity.this,
                                        "E-mail enviado. Verifique sua caixa de entrada.",
                                        Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                if (isDestroyed()) return;
                                Toast.makeText(PerfilActivity.this,
                                        AuthRepository.friendlyError(e),
                                        Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }
}

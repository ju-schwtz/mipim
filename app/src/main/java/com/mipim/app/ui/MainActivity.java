package com.mipim.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mipim.app.R;
import com.mipim.app.data.AuthRepository;
import com.mipim.app.data.ColecaoRepository;
import com.mipim.app.databinding.ActivityMainBinding;
import com.mipim.app.databinding.DialogAddColecaoBinding;
import com.mipim.app.model.Colecao;

import java.util.ArrayList;
import java.util.List;

/** Tela principal: Biblioteca do usuário (busca, filtros, adicionar e remover) */
public class MainActivity extends AppCompatActivity {

    private static final String[] TIPOS = {
            Colecao.TIPO_MANGA, Colecao.TIPO_HQ, Colecao.TIPO_LIVRO, Colecao.TIPO_N0VEL};
    private static final String[] ROTULOS = {"Mangá", "HQ", "Livro", "Novel"};

    private ActivityMainBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private final ColecaoRepository repository = new ColecaoRepository();
    private ColecaoAdapter adapter;

    /** Todas as coleções do usuário (antes de busca e filtro) */
    private List<Colecao> todas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sem sessão: volta para o login
        if (!authRepository.isLoggedIn()) {
            irParaLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new ColecaoAdapter(new ColecaoAdapter.Listener() {
            @Override
            public void onClick(Colecao colecao) {
                Intent intent = new Intent(MainActivity.this, ColecaoDetalheActivity.class);
                intent.putExtra(ColecaoDetalheActivity.EXTRA_COLECAO_ID, colecao.getId());
                startActivity(intent);
            }

            @Override
            public void onRemover(Colecao colecao) {
                confirmarRemocao(colecao);
            }

            @Override
            public void onNova() {
                mostrarDialogAdicionar();
            }
        });
        binding.rvColecoes.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvColecoes.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { aplicarFiltros(); }
        });
        binding.chipGroup.setOnCheckedStateChangeListener((group, ids) -> aplicarFiltros());

        binding.fabAdd.setOnClickListener(v -> mostrarDialogAdicionar());
        Navegacao.configurar(this, binding.navBar.bottomNav, R.id.nav_biblioteca);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) return;

        // Listener preso à Activity: é removido sozinho no onStop()
        repository.observar(this, new ColecaoRepository.ListListener() {
            @Override
            public void onChange(@NonNull List<Colecao> colecoes) {
                binding.progress.setVisibility(View.GONE);
                todas = colecoes;
                aplicarFiltros();
            }

            @Override
            public void onError(@NonNull Exception e) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this,
                        ColecaoRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---------- Busca e filtros (histórias 19 e 20) ----------

    private void aplicarFiltros() {
        String busca = ColecaoRepository.normalizar(binding.etSearch.getText().toString());
        int filtro = binding.chipGroup.getCheckedChipId();

        List<Colecao> resultado = new ArrayList<>();
        for (Colecao c : todas) {
            String titulo = c.getTituloBusca();
            if (!busca.isEmpty() && (titulo == null || !titulo.contains(busca))) continue;

            if (filtro == R.id.chipCompletas && !c.isCompleta()) continue;
            if (filtro == R.id.chipIncompletas
                    && !(c.getVolumesAdquiridos() > 0 && !c.isCompleta())) continue;
            if (filtro == R.id.chipFaltantes && c.isCompleta()) continue;

            resultado.add(c);
        }

        adapter.submit(resultado);

        if (resultado.isEmpty()) {
            binding.tvVazio.setText(todas.isEmpty()
                    ? "Você ainda não tem coleções.\nToque em + para adicionar."
                    : "Nenhuma coleção encontrada.");
            binding.tvVazio.setVisibility(View.VISIBLE);
        } else {
            binding.tvVazio.setVisibility(View.GONE);
        }
    }

    // ---------- Adicionar coleção (história 4) ----------

    private void mostrarDialogAdicionar() {
        DialogAddColecaoBinding d = DialogAddColecaoBinding.inflate(getLayoutInflater());
        d.spTipo.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ROTULOS));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Nova coleção")
                .setView(d.getRoot())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Adicionar", null) // clique definido abaixo
                .create();
        dialog.show();

        // Assim o diálogo não fecha sozinho quando a validação falha
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String titulo = texto(d.etTitulo);
            String autor = texto(d.etAutor);
            final int total = lerInteiro(texto(d.etTotal));
            String tipo = TIPOS[d.spTipo.getSelectedItemPosition()];

            d.tilTitulo.setError(null);
            d.tilTotal.setError(null);

            boolean valido = true;
            if (titulo.isEmpty()) {
                d.tilTitulo.setError("Informe o título");
                valido = false;
            }
            if (total < 1 || total > ColecaoRepository.MAX_VOLUMES) {
                d.tilTotal.setError("Informe de 1 a " + ColecaoRepository.MAX_VOLUMES);
                valido = false;
            }
            if (!valido) return;

            btn.setEnabled(false);
            repository.adicionar(titulo, autor, tipo, total, new ColecaoRepository.Callback() {
                @Override
                public void onSuccess() {
                    if (isDestroyed()) return;
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this,
                            "Coleção adicionada!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    if (isDestroyed()) return;
                    btn.setEnabled(true);
                    String msg = ColecaoRepository.friendlyError(e);
                    if (ColecaoRepository.isDuplicada(e)) {
                        d.tilTitulo.setError(msg);
                    } else {
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    // ---------- Remover coleção (história 6) ----------

    private void confirmarRemocao(Colecao colecao) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remover coleção?")
                .setMessage("\"" + colecao.getTitulo()
                        + "\" e todos os seus volumes serão excluídos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (dialog, which) ->
                        repository.remover(colecao.getId(), new ColecaoRepository.Callback() {
                            @Override
                            public void onSuccess() {
                                if (isDestroyed()) return;
                                Toast.makeText(MainActivity.this,
                                        "Coleção removida.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                if (isDestroyed()) return;
                                Toast.makeText(MainActivity.this,
                                        ColecaoRepository.friendlyError(e),
                                        Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    // ---------- Auxiliares ----------

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String texto(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static int lerInteiro(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

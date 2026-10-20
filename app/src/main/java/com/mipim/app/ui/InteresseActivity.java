package com.mipim.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mipim.app.R;
import com.mipim.app.data.ColecaoRepository;
import com.mipim.app.data.ListaRepository;
import com.mipim.app.databinding.ActivityInteresseBinding;
import com.mipim.app.model.Volume;

import java.util.List;

/** Histórias 13 e 14: lista de interesse com prioridade de compra */
public class InteresseActivity extends AppCompatActivity {

    private static final String[] PRIO_VALORES = {
            Volume.PRIO_ALTA, Volume.PRIO_MEDIA, Volume.PRIO_BAIXA};
    private static final String[] PRIO_ROTULOS = {"Alta", "Média", "Baixa"};

    private ActivityInteresseBinding binding;
    private final ListaRepository repository = new ListaRepository();
    private InteresseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInteresseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Navegacao.configurar(this, binding.navBar.bottomNav, R.id.nav_desejos);

        adapter = new InteresseAdapter(new InteresseAdapter.Listener() {
            @Override
            public void onAlterarPrioridade(Volume volume) {
                escolherPrioridade(volume);
            }

            @Override
            public void onRemover(Volume volume) {
                repository.definirInteresse(volume, false, null,
                        resultado("Removido da lista de interesse"));
            }
        });
        binding.rvInteresse.setLayoutManager(new LinearLayoutManager(this));
        binding.rvInteresse.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        repository.observarDesejados(this, new ListaRepository.VolumesListener() {
            @Override
            public void onChange(@NonNull List<Volume> volumes) {
                binding.progress.setVisibility(View.GONE);
                adapter.submit(volumes);

                if (volumes.isEmpty()) {
                    binding.tvTotal.setText("");
                    binding.tvVazio.setVisibility(View.VISIBLE);
                } else {
                    binding.tvVazio.setVisibility(View.GONE);
                    binding.tvTotal.setText(volumes.size()
                            + (volumes.size() == 1 ? " item" : " itens") + " na lista");
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(InteresseActivity.this,
                        ListaRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** História 14: alterar a prioridade (alta, média ou baixa) */
    private void escolherPrioridade(Volume volume) {
        int atual = 1; // média por padrão
        for (int i = 0; i < PRIO_VALORES.length; i++) {
            if (PRIO_VALORES[i].equals(volume.getPrioridade())) atual = i;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Prioridade de compra")
                .setSingleChoiceItems(PRIO_ROTULOS, atual, (dialog, which) -> {
                    dialog.dismiss();
                    repository.definirInteresse(volume, true, PRIO_VALORES[which],
                            resultado(null));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Callback comum: mostra mensagem de sucesso (se houver) ou o erro */
    private ColecaoRepository.Callback resultado(String mensagemSucesso) {
        return new ColecaoRepository.Callback() {
            @Override
            public void onSuccess() {
                if (isDestroyed() || mensagemSucesso == null) return;
                Toast.makeText(InteresseActivity.this,
                        mensagemSucesso, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (isDestroyed()) return;
                Toast.makeText(InteresseActivity.this,
                        ListaRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        };
    }
}

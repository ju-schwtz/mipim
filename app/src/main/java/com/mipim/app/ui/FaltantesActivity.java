package com.mipim.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mipim.app.R;
import com.mipim.app.data.ColecaoRepository;
import com.mipim.app.data.ListaRepository;
import com.mipim.app.databinding.ActivityFaltantesBinding;
import com.mipim.app.model.Volume;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** História 11: volumes que ainda faltam, agrupados por coleção */
public class FaltantesActivity extends AppCompatActivity {

    private ActivityFaltantesBinding binding;
    private final ListaRepository repository = new ListaRepository();
    private FaltantesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaltantesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Navegacao.configurar(this, binding.navBar.bottomNav, R.id.nav_faltantes);

        adapter = new FaltantesAdapter(new FaltantesAdapter.Listener() {
            @Override
            public void onAbrirColecao(String colecaoId) {
                Intent intent = new Intent(FaltantesActivity.this, ColecaoDetalheActivity.class);
                intent.putExtra(ColecaoDetalheActivity.EXTRA_COLECAO_ID, colecaoId);
                startActivity(intent);
            }

            @Override
            public void onAlternarInteresse(Volume volume) {
                alternarInteresse(volume);
            }
        });
        binding.rvFaltantes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFaltantes.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Quando um volume é marcado como adquirido, ele sai desta lista sozinho
        repository.observarFaltantes(this, new ListaRepository.VolumesListener() {
            @Override
            public void onChange(@NonNull List<Volume> volumes) {
                binding.progress.setVisibility(View.GONE);
                adapter.submit(volumes);
                atualizarTotal(volumes);
            }

            @Override
            public void onError(@NonNull Exception e) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(FaltantesActivity.this,
                        ListaRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void atualizarTotal(List<Volume> volumes) {
        Set<String> colecoes = new HashSet<>();
        for (Volume v : volumes) colecoes.add(v.getColecaoId());

        if (volumes.isEmpty()) {
            binding.tvTotal.setText("");
            binding.tvVazio.setVisibility(View.VISIBLE);
        } else {
            binding.tvVazio.setVisibility(View.GONE);
            binding.tvTotal.setText(volumes.size()
                    + (volumes.size() == 1 ? " volume faltando" : " volumes faltando")
                    + " em " + colecoes.size()
                    + (colecoes.size() == 1 ? " coleção" : " coleções"));
        }
    }

    /** História 13: adiciona o volume à lista de interesse (prioridade média) ou remove */
    private void alternarInteresse(Volume volume) {
        final boolean adicionar = !volume.isDesejado();
        repository.definirInteresse(volume, adicionar, Volume.PRIO_MEDIA,
                new ColecaoRepository.Callback() {
                    @Override
                    public void onSuccess() {
                        if (isDestroyed()) return;
                        Toast.makeText(FaltantesActivity.this,
                                adicionar ? "Adicionado à lista de interesse"
                                        : "Removido da lista de interesse",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        if (isDestroyed()) return;
                        Toast.makeText(FaltantesActivity.this,
                                ListaRepository.friendlyError(e), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

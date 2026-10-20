package com.mipim.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mipim.app.data.Financeiro;
import com.mipim.app.data.ListaRepository;
import com.mipim.app.databinding.ActivityFinanceiroBinding;
import com.mipim.app.model.Volume;

import java.util.List;

/** Histórias 15 e 16: total gasto e gastos por mês */
public class FinanceiroActivity extends AppCompatActivity {

    private ActivityFinanceiroBinding binding;
    private final ListaRepository repository = new ListaRepository();
    private final GastosAdapter adapter = new GastosAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinanceiroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnVoltar.setOnClickListener(v -> finish());
        binding.rvMeses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMeses.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Em tempo real: qualquer compra adicionada ou alterada recalcula tudo
        repository.observarAdquiridos(this, new ListaRepository.VolumesListener() {
            @Override
            public void onChange(@NonNull List<Volume> volumes) {
                binding.progress.setVisibility(View.GONE);

                Financeiro.Resultado r = Financeiro.calcular(volumes);
                binding.tvTotalGasto.setText(Formatos.moeda(r.total));
                binding.tvCompras.setText(r.compras
                        + (r.compras == 1 ? " compra registrada" : " compras registradas"));

                adapter.submit(r);
                binding.tvVazio.setVisibility(r.compras == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(@NonNull Exception e) {
                binding.progress.setVisibility(View.GONE);
                Toast.makeText(FinanceiroActivity.this,
                        ListaRepository.friendlyError(e), Toast.LENGTH_LONG).show();
            }
        });
    }
}

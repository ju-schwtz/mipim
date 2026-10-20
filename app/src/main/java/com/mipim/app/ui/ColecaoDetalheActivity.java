package com.mipim.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.mipim.app.data.ColecaoRepository;
import com.mipim.app.data.VolumeRepository;
import com.mipim.app.databinding.ActivityColecaoDetalheBinding;
import com.mipim.app.databinding.DialogVolumeBinding;
import com.mipim.app.model.Colecao;
import com.mipim.app.model.Volume;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Detalhes de uma coleção: resumo, progresso e lista de volumes */
public class ColecaoDetalheActivity extends AppCompatActivity {

    public static final String EXTRA_COLECAO_ID = "colecaoId";

    private static final String[] CONS_VALORES = {
            null, Volume.CONS_NOVO, Volume.CONS_BOM, Volume.CONS_REGULAR, Volume.CONS_RUIM};
    private static final String[] CONS_ROTULOS = {
            "Não informado", "Novo", "Bom", "Regular", "Ruim"};

    private ActivityColecaoDetalheBinding binding;
    private final VolumeRepository repository = new VolumeRepository();
    private VolumeAdapter adapter;
    private String colecaoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        colecaoId = getIntent().getStringExtra(EXTRA_COLECAO_ID);
        if (colecaoId == null) {
            finish();
            return;
        }

        binding = ActivityColecaoDetalheBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnVoltar.setOnClickListener(v -> finish());

        adapter = new VolumeAdapter(new VolumeAdapter.Listener() {
            @Override
            public void onAbrir(Volume volume) {
                mostrarDialogVolume(volume);
            }

            @Override
            public void onAlternar(Volume volume, boolean adquirido) {
                Map<String, Object> campos = new HashMap<>();
                campos.put("adquirido", adquirido);
                repository.atualizar(colecaoId, volume.getId(), campos,
                        new ColecaoRepository.Callback() {
                            @Override
                            public void onSuccess() { /* a lista atualiza sozinha */ }

                            @Override
                            public void onError(@NonNull Exception e) {
                                if (isDestroyed()) return;
                                adapter.refresh(); // desfaz o visual da caixa
                                Toast.makeText(ColecaoDetalheActivity.this,
                                        ColecaoRepository.friendlyError(e),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        binding.rvVolumes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVolumes.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) return;

        repository.observarColecao(this, colecaoId, new VolumeRepository.ColecaoListener() {
            @Override
            public void onChange(Colecao colecao) {
                if (colecao == null) { // removida
                    finish();
                    return;
                }
                preencherResumo(colecao);
            }

            @Override
            public void onError(@NonNull Exception e) {
                mostrarErro(e);
            }
        });

        repository.observarVolumes(this, colecaoId, new VolumeRepository.VolumesListener() {
            @Override
            public void onChange(@NonNull List<Volume> volumes) {
                binding.progress.setVisibility(View.GONE);
                adapter.submit(volumes);
            }

            @Override
            public void onError(@NonNull Exception e) {
                binding.progress.setVisibility(View.GONE);
                mostrarErro(e);
            }
        });
    }

    /** Histórias 5 e 12: dados da obra, adquiridos, faltantes e percentual */
    private void preencherResumo(Colecao c) {
        binding.tvTitulo.setText(c.getTitulo());

        String info = ColecaoAdapter.rotuloTipo(c.getTipo());
        if (c.getAutor() != null && !c.getAutor().isEmpty()) info += " • " + c.getAutor();
        binding.tvInfo.setText(info);

        binding.tvStatTotal.setText(String.valueOf(c.getTotalVolumes()));
        binding.tvStatAdquiridos.setText(String.valueOf(c.getVolumesAdquiridos()));
        binding.tvStatFaltantes.setText(String.valueOf(c.getVolumesFaltantes()));
        binding.progressColecao.setProgress(c.getProgresso());
        binding.tvPercentual.setText(c.getProgresso() + "% concluído");
    }

    // ---------- Formulário do volume (histórias 8, 9 e 10) ----------

    private void mostrarDialogVolume(Volume v) {
        DialogVolumeBinding d = DialogVolumeBinding.inflate(getLayoutInflater());
        d.spConservacao.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CONS_ROTULOS));

        // Data escolhida (milissegundos em UTC); null = sem data
        final Long[] data = { v.getDataCompra() != null
                ? v.getDataCompra().toDate().getTime() : null };

        // Preenche com o que já está salvo (permite consultar e editar)
        d.swAdquirido.setChecked(v.isAdquirido());
        d.layoutCompra.setVisibility(v.isAdquirido() ? View.VISIBLE : View.GONE);
        if (v.getValorPago() != null) d.etValor.setText(Formatos.valorEditavel(v.getValorPago()));
        if (data[0] != null) d.etData.setText(Formatos.data(data[0]));
        d.etLocal.setText(v.getLocalCompra());
        d.spConservacao.setSelection(indiceConservacao(v.getConservacao()));

        d.swAdquirido.setOnCheckedChangeListener((b, marcado) ->
                d.layoutCompra.setVisibility(marcado ? View.VISIBLE : View.GONE));

        d.etData.setOnClickListener(x -> {
            MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Data da compra");
            if (data[0] != null) builder.setSelection(data[0]);
            MaterialDatePicker<Long> picker = builder.build();
            picker.addOnPositiveButtonClickListener(sel -> {
                data[0] = sel;
                d.etData.setText(Formatos.data(sel));
            });
            picker.show(getSupportFragmentManager(), "data_compra");
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Volume " + v.getNumero())
                .setView(d.getRoot())
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            boolean adquirido = d.swAdquirido.isChecked();
            Map<String, Object> campos = new HashMap<>();
            campos.put("adquirido", adquirido);

            // Se não está adquirido, os dados de compra salvos antes são preservados
            if (adquirido) {
                d.tilValor.setError(null);
                Double valor = null;
                String textoValor = texto(d.etValor);
                if (!textoValor.isEmpty()) {
                    try {
                        valor = Double.parseDouble(textoValor.replace(",", "."));
                        if (valor < 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        d.tilValor.setError("Valor inválido");
                        return;
                    }
                }
                String local = texto(d.etLocal);

                campos.put("valorPago", valor);
                campos.put("dataCompra",
                        data[0] == null ? null : new Timestamp(new Date(data[0])));
                campos.put("localCompra", local.isEmpty() ? null : local);
                campos.put("conservacao", CONS_VALORES[d.spConservacao.getSelectedItemPosition()]);
            }

            btn.setEnabled(false);
            repository.atualizar(colecaoId, v.getId(), campos, new ColecaoRepository.Callback() {
                @Override
                public void onSuccess() {
                    if (isDestroyed()) return;
                    dialog.dismiss();
                    Toast.makeText(ColecaoDetalheActivity.this,
                            "Volume salvo!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    if (isDestroyed()) return;
                    btn.setEnabled(true);
                    mostrarErro(e);
                }
            });
        });
    }

    // ---------- Auxiliares ----------

    private void mostrarErro(Exception e) {
        Toast.makeText(this, ColecaoRepository.friendlyError(e), Toast.LENGTH_LONG).show();
    }

    private static int indiceConservacao(String conservacao) {
        for (int i = 0; i < CONS_VALORES.length; i++) {
            if (CONS_VALORES[i] == null ? conservacao == null
                    : CONS_VALORES[i].equals(conservacao)) {
                return i;
            }
        }
        return 0;
    }

    private static String texto(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}

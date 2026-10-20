package com.mipim.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mipim.app.R;
import com.mipim.app.data.AuthRepository;
import com.mipim.app.data.ColecaoRepository;
import com.mipim.app.data.Financeiro;
import com.mipim.app.data.ListaRepository;
import com.mipim.app.data.PerfilRepository;
import com.mipim.app.databinding.ActivityDashboardBinding;
import com.mipim.app.model.Colecao;
import com.mipim.app.model.Volume;

import java.util.Calendar;
import java.util.List;

/** História 18: tela inicial com o resumo da biblioteca e atalhos */
public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private final ColecaoRepository colecaoRepository = new ColecaoRepository();
    private final ListaRepository listaRepository = new ListaRepository();
    private final PerfilRepository perfilRepository = new PerfilRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!authRepository.isLoggedIn()) {
            irParaLogin();
            return;
        }

        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGastos.setOnClickListener(v -> abrir(FinanceiroActivity.class));
        Navegacao.configurar(this, binding.navBar.bottomNav, R.id.nav_inicio);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (binding == null) return;

        // Nome do usuário
        perfilRepository.observar(this, new PerfilRepository.PerfilListener() {
            @Override
            public void onChange(String nome, String email) {
                if (nome != null && !nome.trim().isEmpty()) {
                    binding.tvOla.setText("Olá, " + nome.trim().split("\\s+")[0] + "!");
                } else {
                    binding.tvOla.setText("Olá!");
                }
            }

            @Override
            public void onError(@NonNull Exception e) { /* o nome é opcional aqui */ }
        });

        // Coleções, volumes e progresso (vêm dos contadores de cada coleção)
        colecaoRepository.observar(this, new ColecaoRepository.ListListener() {
            @Override
            public void onChange(@NonNull List<Colecao> colecoes) {
                atualizarResumo(colecoes);
            }

            @Override
            public void onError(@NonNull Exception e) {
                mostrarErro(ColecaoRepository.friendlyError(e));
            }
        });

        // Gastos (volumes adquiridos com valor registrado)
        listaRepository.observarAdquiridos(this, new ListaRepository.VolumesListener() {
            @Override
            public void onChange(@NonNull List<Volume> volumes) {
                Financeiro.Resultado r = Financeiro.calcular(volumes);

                Calendar hoje = Calendar.getInstance();
                double doMes = r.gastoDoMes(hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH));

                binding.tvTotalGasto.setText(Formatos.moeda(r.total));
                binding.tvGastoMes.setText("Neste mês: " + Formatos.moeda(doMes));
            }

            @Override
            public void onError(@NonNull Exception e) {
                mostrarErro(ListaRepository.friendlyError(e));
            }
        });
    }

    private void atualizarResumo(List<Colecao> colecoes) {
        int totalVolumes = 0;
        int adquiridos = 0;
        for (Colecao c : colecoes) {
            totalVolumes += c.getTotalVolumes();
            adquiridos += c.getVolumesAdquiridos();
        }
        int faltantes = Math.max(totalVolumes - adquiridos, 0);
        int progresso = totalVolumes == 0 ? 0 : Math.round(adquiridos * 100f / totalVolumes);

        binding.tvColecoes.setText(String.valueOf(colecoes.size()));
        binding.tvTotalVolumes.setText(String.valueOf(totalVolumes));
        binding.tvAdquiridos.setText(String.valueOf(adquiridos));
        binding.tvFaltantes.setText(String.valueOf(faltantes));
        binding.progressGeral.setProgress(progresso);
        binding.tvProgressoGeral.setText(progresso + "% concluído");
        binding.tvAviso.setVisibility(colecoes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrir(Class<?> destino) {
        startActivity(new Intent(this, destino));
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void mostrarErro(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }
}

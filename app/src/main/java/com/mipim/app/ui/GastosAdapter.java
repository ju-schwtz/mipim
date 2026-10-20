package com.mipim.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mipim.app.data.Financeiro;
import com.mipim.app.databinding.ItemGastoMesBinding;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista de gastos por mês, com barra proporcional ao maior mês */
public class GastosAdapter extends RecyclerView.Adapter<GastosAdapter.VH> {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private static class Linha {
        String rotulo;
        double total;
        int quantidade;
        int percentual; // 0 a 100, em relação ao maior valor da lista
    }

    private final List<Linha> linhas = new ArrayList<>();

    public void submit(Financeiro.Resultado r) {
        linhas.clear();
        double maior = 0;

        for (Financeiro.Mes m : r.meses) {
            Linha l = new Linha();
            l.rotulo = rotuloMes(m.ano, m.mes);
            l.total = m.total;
            l.quantidade = m.quantidade;
            linhas.add(l);
            maior = Math.max(maior, m.total);
        }

        if (r.semDataQtd > 0) {
            Linha l = new Linha();
            l.rotulo = "Sem data informada";
            l.total = r.semData;
            l.quantidade = r.semDataQtd;
            linhas.add(l);
            maior = Math.max(maior, r.semData);
        }

        for (Linha l : linhas) {
            l.percentual = maior > 0 ? (int) Math.round(l.total * 100 / maior) : 0;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemGastoMesBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Linha l = linhas.get(position);
        ItemGastoMesBinding b = h.binding;

        b.tvMes.setText(l.rotulo);
        b.tvValor.setText(Formatos.moeda(l.total));
        b.tvQtd.setText(l.quantidade + (l.quantidade == 1 ? " volume" : " volumes"));
        b.progressMes.setProgress(l.percentual);
    }

    @Override
    public int getItemCount() {
        return linhas.size();
    }

    /** (2026, 9) -> "Outubro de 2026" */
    private static String rotuloMes(int ano, int mes) {
        String nome = new DateFormatSymbols(BR).getMonths()[mes];
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " de " + ano;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemGastoMesBinding binding;

        VH(ItemGastoMesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

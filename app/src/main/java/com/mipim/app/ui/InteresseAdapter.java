package com.mipim.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mipim.app.R;
import com.mipim.app.databinding.ItemInteresseBinding;
import com.mipim.app.model.Volume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lista de interesse, ordenada da prioridade mais alta para a mais baixa */
public class InteresseAdapter extends RecyclerView.Adapter<InteresseAdapter.VH> {

    public interface Listener {
        void onAlterarPrioridade(Volume volume);
        void onRemover(Volume volume);
    }

    private final List<Volume> itens = new ArrayList<>();
    private final Listener listener;

    public InteresseAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Volume> volumes) {
        List<Volume> ordenados = new ArrayList<>(volumes);
        Collections.sort(ordenados, (a, b) -> {
            int c = Integer.compare(peso(a.getPrioridade()), peso(b.getPrioridade()));
            if (c != 0) return c;
            c = nz(a.getColecaoTitulo()).compareToIgnoreCase(nz(b.getColecaoTitulo()));
            return c != 0 ? c : Integer.compare(a.getNumero(), b.getNumero());
        });
        itens.clear();
        itens.addAll(ordenados);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemInteresseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Volume v = itens.get(position);
        ItemInteresseBinding b = h.binding;

        b.tvColecao.setText(v.getColecaoTitulo());
        b.tvNumero.setText("Volume " + v.getNumero());

        // Alta = preenchido (Crimson Violet); Média e Baixa = contorno
        Context ctx = b.getRoot().getContext();
        int cor = ContextCompat.getColor(ctx, corPrioridade(v.getPrioridade()));
        int branco = ContextCompat.getColor(ctx, R.color.white);
        boolean preenchido = Volume.PRIO_ALTA.equals(v.getPrioridade());

        b.btnPrioridade.setText(rotuloPrioridade(v.getPrioridade()));
        b.btnPrioridade.setStrokeColor(ColorStateList.valueOf(cor));
        b.btnPrioridade.setTextColor(preenchido ? branco : cor);
        b.btnPrioridade.setBackgroundTintList(ColorStateList.valueOf(preenchido ? cor : branco));

        b.btnPrioridade.setOnClickListener(x -> listener.onAlterarPrioridade(v));
        b.btnRemover.setOnClickListener(x -> listener.onRemover(v));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    // ---------- Prioridade ----------

    private static int peso(String prioridade) {
        if (Volume.PRIO_ALTA.equals(prioridade)) return 0;
        if (Volume.PRIO_BAIXA.equals(prioridade)) return 2;
        return 1; // média (ou não definida)
    }

    static String rotuloPrioridade(String prioridade) {
        if (Volume.PRIO_ALTA.equals(prioridade)) return "Alta";
        if (Volume.PRIO_BAIXA.equals(prioridade)) return "Baixa";
        return "Média";
    }

    /** Devolve o recurso de cor (R.color.*) da prioridade */
    private static int corPrioridade(String prioridade) {
        if (Volume.PRIO_ALTA.equals(prioridade)) return R.color.crimson_violet;
        if (Volume.PRIO_BAIXA.equals(prioridade)) return R.color.dusty_mauve;
        return R.color.vintage_berry;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemInteresseBinding binding;

        VH(ItemInteresseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

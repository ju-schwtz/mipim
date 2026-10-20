package com.mipim.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mipim.app.R;
import com.mipim.app.databinding.ItemVolumeBinding;
import com.mipim.app.model.Volume;

import java.util.ArrayList;
import java.util.List;

/** Lista de volumes de uma coleção, com destaque entre adquiridos e faltantes */
public class VolumeAdapter extends RecyclerView.Adapter<VolumeAdapter.VH> {

    public interface Listener {
        /** Toque no card: abre o formulário completo do volume */
        void onAbrir(Volume volume);
        /** Toque na caixa de seleção: marca/desmarca como adquirido */
        void onAlternar(Volume volume, boolean adquirido);
    }

    private final List<Volume> itens = new ArrayList<>();
    private final Listener listener;

    public VolumeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Volume> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    /** Redesenha a lista (usado para desfazer o visual da caixa quando a gravação falha) */
    public void refresh() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemVolumeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Volume v = itens.get(position);
        ItemVolumeBinding b = h.binding;
        Context ctx = b.getRoot().getContext();

        b.tvNumero.setText("Volume " + v.getNumero());
        b.cbAdquirido.setChecked(v.isAdquirido());

        if (v.isAdquirido()) {
            b.tvStatus.setText("Adquirido");
            b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.white));
            b.tvStatus.setBackgroundResource(R.drawable.bg_status_adquirido);
            b.getRoot().setAlpha(1f);

            List<String> partes = new ArrayList<>();
            if (v.getValorPago() != null) partes.add(Formatos.moeda(v.getValorPago()));
            if (v.getDataCompra() != null) partes.add(Formatos.data(v.getDataCompra()));
            if (v.getLocalCompra() != null && !v.getLocalCompra().isEmpty()) {
                partes.add(v.getLocalCompra());
            }
            if (v.getConservacao() != null) {
                partes.add("Conservação: " + Formatos.rotuloConservacao(v.getConservacao()));
            }
            b.tvDetalhes.setText(partes.isEmpty()
                    ? "Toque para registrar compra e conservação"
                    : String.join(" • ", partes));
            b.tvDetalhes.setVisibility(View.VISIBLE);
        } else {
            b.tvStatus.setText("Faltando");
            b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.dusty_mauve));
            b.tvStatus.setBackgroundResource(R.drawable.bg_status_faltando);
            b.getRoot().setAlpha(1f);
            b.tvDetalhes.setVisibility(View.GONE);
        }

        b.getRoot().setOnClickListener(x -> listener.onAbrir(v));
        b.cbAdquirido.setOnClickListener(x ->
                listener.onAlternar(v, b.cbAdquirido.isChecked()));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemVolumeBinding binding;

        VH(ItemVolumeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

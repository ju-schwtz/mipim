package com.mipim.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mipim.app.databinding.ItemColecaoBinding;
import com.mipim.app.databinding.ItemNovaColecaoBinding;
import com.mipim.app.model.Colecao;

import java.util.ArrayList;
import java.util.List;

/**
 * Grade de coleções da Biblioteca: um cartão por coleção
 * e, no fim, o cartão tracejado "Nova coleção".
 */
public class ColecaoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onClick(Colecao colecao);
        void onRemover(Colecao colecao);
        void onNova();
    }

    private static final int TIPO_COLECAO = 0;
    private static final int TIPO_NOVA = 1;

    private final List<Colecao> itens = new ArrayList<>();
    private final Listener listener;

    public ColecaoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Colecao> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position < itens.size() ? TIPO_COLECAO : TIPO_NOVA;
    }

    @Override
    public int getItemCount() {
        return itens.size() + 1; // +1 = cartão "Nova coleção"
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_NOVA) {
            return new NovaVH(ItemNovaColecaoBinding.inflate(inflater, parent, false));
        }
        return new ColecaoVH(ItemColecaoBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NovaVH) {
            ((NovaVH) holder).binding.getRoot().setOnClickListener(v -> listener.onNova());
            return;
        }

        Colecao c = itens.get(position);
        ItemColecaoBinding b = ((ColecaoVH) holder).binding;

        b.tvTitulo.setText(c.getTitulo());

        String autor = c.getAutor();
        String info = rotuloTipo(c.getTipo());
        if (autor != null && !autor.isEmpty()) info += " • " + autor;
        b.tvInfo.setText(info);

        b.tvVolumes.setText(c.getVolumesAdquiridos() + " de " + c.getTotalVolumes() + " volumes");
        b.progressColecao.setProgress(c.getProgresso());
        b.tvPercentual.setText(c.getProgresso() + "%");

        b.getRoot().setOnClickListener(v -> listener.onClick(c));
        b.btnRemover.setOnClickListener(v -> listener.onRemover(c));
    }

    static String rotuloTipo(String tipo) {
        if (tipo == null) return "";
        switch (tipo) {
            case Colecao.TIPO_MANGA: return "Mangá";
            case Colecao.TIPO_HQ: return "HQ";
            case Colecao.TIPO_LIVRO: return "Livro";
            case Colecao.TIPO_N0VEL: return "Novel";
            default: return tipo;
        }
    }

    static class ColecaoVH extends RecyclerView.ViewHolder {
        final ItemColecaoBinding binding;

        ColecaoVH(ItemColecaoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class NovaVH extends RecyclerView.ViewHolder {
        final ItemNovaColecaoBinding binding;

        NovaVH(ItemNovaColecaoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

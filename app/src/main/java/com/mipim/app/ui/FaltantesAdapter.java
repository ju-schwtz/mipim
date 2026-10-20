package com.mipim.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mipim.app.databinding.ItemFaltanteBinding;
import com.mipim.app.databinding.ItemGrupoBinding;
import com.mipim.app.model.Volume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Volumes faltantes agrupados por coleção (cabeçalho + volumes) */
public class FaltantesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onAbrirColecao(String colecaoId);
        void onAlternarInteresse(Volume volume);
    }

    private static final int TIPO_GRUPO = 0;
    private static final int TIPO_VOLUME = 1;

    /** Uma linha da lista: cabeçalho de grupo (volume == null) ou um volume */
    private static class Linha {
        String colecaoId;
        String titulo;
        int quantidade;
        Volume volume;
    }

    private final List<Linha> linhas = new ArrayList<>();
    private final Listener listener;

    public FaltantesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Volume> volumes) {
        List<Volume> ordenados = new ArrayList<>(volumes);
        Collections.sort(ordenados, (a, b) -> {
            int c = nz(a.getColecaoTitulo()).compareToIgnoreCase(nz(b.getColecaoTitulo()));
            return c != 0 ? c : Integer.compare(a.getNumero(), b.getNumero());
        });

        // Agrupa mantendo a ordem
        Map<String, List<Volume>> grupos = new LinkedHashMap<>();
        for (Volume v : ordenados) {
            String chave = v.getColecaoId() == null ? "" : v.getColecaoId();
            if (!grupos.containsKey(chave)) grupos.put(chave, new ArrayList<>());
            grupos.get(chave).add(v);
        }

        linhas.clear();
        for (Map.Entry<String, List<Volume>> g : grupos.entrySet()) {
            List<Volume> doGrupo = g.getValue();

            Linha cabecalho = new Linha();
            cabecalho.colecaoId = g.getKey();
            cabecalho.titulo = doGrupo.get(0).getColecaoTitulo();
            cabecalho.quantidade = doGrupo.size();
            linhas.add(cabecalho);

            for (Volume v : doGrupo) {
                Linha item = new Linha();
                item.colecaoId = g.getKey();
                item.volume = v;
                linhas.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return linhas.get(position).volume == null ? TIPO_GRUPO : TIPO_VOLUME;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_GRUPO) {
            return new GrupoVH(ItemGrupoBinding.inflate(inflater, parent, false));
        }
        return new VolumeVH(ItemFaltanteBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Linha l = linhas.get(position);

        if (holder instanceof GrupoVH) {
            ItemGrupoBinding b = ((GrupoVH) holder).binding;
            b.tvGrupoTitulo.setText(l.titulo);
            b.tvGrupoQtd.setText(l.quantidade + " faltando");
            b.getRoot().setOnClickListener(v -> listener.onAbrirColecao(l.colecaoId));
        } else {
            ItemFaltanteBinding b = ((VolumeVH) holder).binding;
            Volume v = l.volume;
            b.tvNumero.setText("Volume " + v.getNumero());
            b.btnInteresse.setText(v.isDesejado() ? "Na lista ✓" : "+ Lista de interesse");
            b.btnInteresse.setOnClickListener(x -> listener.onAlternarInteresse(v));
        }
    }

    @Override
    public int getItemCount() {
        return linhas.size();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    static class GrupoVH extends RecyclerView.ViewHolder {
        final ItemGrupoBinding binding;

        GrupoVH(ItemGrupoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class VolumeVH extends RecyclerView.ViewHolder {
        final ItemFaltanteBinding binding;

        VolumeVH(ItemFaltanteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

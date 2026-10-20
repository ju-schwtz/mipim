package com.mipim.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Uma obra/série que o usuário acompanha (ex.: "Naruto").
 * Caminho no Firestore: users/{uid}/colecoes/{colecaoId}
 */
public class Colecao {

    public static final String TIPO_MANGA = "MANGA";
    public static final String TIPO_HQ = "HQ";
    public static final String TIPO_LIVRO = "LIVRO";
    public static final String TIPO_N0VEL = "NOVEL";

    @DocumentId
    private String id;          // preenchido automaticamente pelo Firestore
    private String ownerId;     // uid do dono (usuário logado)
    private String titulo;
    private String tituloBusca; // título em minúsculas, usado na pesquisa
    private String autor;
    private String tipo;        // MANGA, HQ, LIVRO ou NOVEL
    private int totalVolumes;
    private int volumesAdquiridos; // contador mantido a cada mudança de volume
    @ServerTimestamp
    private Timestamp createdAt;

    public Colecao() { } // obrigatório para o Firestore

    // ---------- Regras de negócio (não são gravadas no banco) ----------

    @Exclude
    public int getVolumesFaltantes() {
        return Math.max(totalVolumes - volumesAdquiridos, 0);
    }

    /** História 12: percentual de conclusão (0 a 100) */
    @Exclude
    public int getProgresso() {
        if (totalVolumes <= 0) return 0;
        return Math.round(volumesAdquiridos * 100f / totalVolumes);
    }

    @Exclude
    public boolean isCompleta() {
        return totalVolumes > 0 && volumesAdquiridos >= totalVolumes;
    }

    // ---------- Getters e setters ----------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getTituloBusca() { return tituloBusca; }
    public void setTituloBusca(String tituloBusca) { this.tituloBusca = tituloBusca; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getTotalVolumes() { return totalVolumes; }
    public void setTotalVolumes(int totalVolumes) { this.totalVolumes = totalVolumes; }

    public int getVolumesAdquiridos() { return volumesAdquiridos; }
    public void setVolumesAdquiridos(int volumesAdquiridos) { this.volumesAdquiridos = volumesAdquiridos; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

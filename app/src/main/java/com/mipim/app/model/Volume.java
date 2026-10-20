package com.mipim.app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Um volume de uma coleção (ex.: "Naruto vol. 5").
 * Caminho no Firestore: users/{uid}/colecoes/{colecaoId}/volumes/{volumeId}
 *
 * Reúne compra, conservação e lista de interesse no mesmo documento,
 * o que simplifica as consultas e evita duplicidade.
 */
public class Volume {

    // Estados de conservação (história 10)
    public static final String CONS_NOVO = "NOVO";
    public static final String CONS_BOM = "BOM";
    public static final String CONS_REGULAR = "REGULAR";
    public static final String CONS_RUIM = "RUIM";

    // Prioridades da lista de interesse (história 14)
    public static final String PRIO_ALTA = "ALTA";
    public static final String PRIO_MEDIA = "MEDIA";
    public static final String PRIO_BAIXA = "BAIXA";

    @DocumentId
    private String id;
    private String ownerId;     // uid do dono
    private String colecaoId;   // a qual coleção pertence
    private String colecaoTitulo; // cópia do título, para listas que misturam coleções
    private int numero;         // número do volume (1, 2, 3...)

    // Aquisição (histórias 8 e 9)
    private boolean adquirido;
    private Double valorPago;   // null enquanto não comprado
    private Timestamp dataCompra;
    private String localCompra;

    // Conservação (história 10)
    private String conservacao;

    // Lista de interesse (histórias 13 e 14)
    private boolean desejado;
    private String prioridade;

    public Volume() { } // obrigatório para o Firestore

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getColecaoId() { return colecaoId; }
    public void setColecaoId(String colecaoId) { this.colecaoId = colecaoId; }

    public String getColecaoTitulo() { return colecaoTitulo; }
    public void setColecaoTitulo(String colecaoTitulo) { this.colecaoTitulo = colecaoTitulo; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public boolean isAdquirido() { return adquirido; }
    public void setAdquirido(boolean adquirido) { this.adquirido = adquirido; }

    public Double getValorPago() { return valorPago; }
    public void setValorPago(Double valorPago) { this.valorPago = valorPago; }

    public Timestamp getDataCompra() { return dataCompra; }
    public void setDataCompra(Timestamp dataCompra) { this.dataCompra = dataCompra; }

    public String getLocalCompra() { return localCompra; }
    public void setLocalCompra(String localCompra) { this.localCompra = localCompra; }

    public String getConservacao() { return conservacao; }
    public void setConservacao(String conservacao) { this.conservacao = conservacao; }

    public boolean isDesejado() { return desejado; }
    public void setDesejado(boolean desejado) { this.desejado = desejado; }

    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }
}

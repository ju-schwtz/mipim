package com.mipim.app.data;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mipim.app.model.Colecao;
import com.mipim.app.model.Volume;

import java.util.List;
import java.util.Map;

/**
 * Acesso aos dados da tela de detalhes: uma coleção e seus volumes.
 * Caminho: users/{uid}/colecoes/{colecaoId}/volumes/{volumeId}
 */
public class VolumeRepository {

    public interface ColecaoListener {
        /** Recebe null quando a coleção deixa de existir (foi removida) */
        void onChange(Colecao colecao);
        void onError(@NonNull Exception e);
    }

    public interface VolumesListener {
        void onChange(@NonNull List<Volume> volumes);
        void onError(@NonNull Exception e);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference colecaoRef(String colecaoId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) throw new IllegalStateException("Usuário não logado");
        return db.collection("users").document(user.getUid())
                .collection("colecoes").document(colecaoId);
    }

    /** História 5: dados da obra e contadores, em tempo real */
    public void observarColecao(Activity activity, String colecaoId, ColecaoListener listener) {
        colecaoRef(colecaoId).addSnapshotListener(activity, (snap, e) -> {
            if (e != null) {
                listener.onError(e);
                return;
            }
            if (snap == null || !snap.exists()) {
                listener.onChange(null);
            } else {
                listener.onChange(snap.toObject(Colecao.class));
            }
        });
    }

    /** História 7: volumes da coleção, em ordem numérica */
    public void observarVolumes(Activity activity, String colecaoId, VolumesListener listener) {
        colecaoRef(colecaoId).collection("volumes").orderBy("numero")
                .addSnapshotListener(activity, (snap, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (snap != null) {
                        listener.onChange(snap.toObjects(Volume.class));
                    }
                });
    }

    /**
     * Histórias 8, 9 e 10: atualiza campos de um volume.
     * Se o campo "adquirido" mudar, o contador volumesAdquiridos da coleção
     * é ajustado na MESMA transação, então os dois nunca ficam inconsistentes.
     */
    public void atualizar(String colecaoId, String volumeId,
                          Map<String, Object> campos, ColecaoRepository.Callback callback) {
        DocumentReference colRef = colecaoRef(colecaoId);
        DocumentReference volRef = colRef.collection("volumes").document(volumeId);

        // Volume adquirido sai da lista de interesse automaticamente
        Map<String, Object> alteracoes = new java.util.HashMap<>(campos);
        if (Boolean.TRUE.equals(campos.get("adquirido"))) {
            alteracoes.put("desejado", false);
            alteracoes.put("prioridade", null);
        }

        db.runTransaction(tx -> {
            DocumentSnapshot atual = tx.get(volRef);
            boolean estavaAdquirido = Boolean.TRUE.equals(atual.getBoolean("adquirido"));

            tx.update(volRef, alteracoes);

            Object novo = campos.get("adquirido");
            if (novo instanceof Boolean && (Boolean) novo != estavaAdquirido) {
                tx.update(colRef, "volumesAdquiridos",
                        FieldValue.increment((Boolean) novo ? 1 : -1));
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(callback::onError);
    }
}

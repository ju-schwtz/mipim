package com.mipim.app.data;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.mipim.app.model.Volume;

import java.util.List;

/**
 * Consultas que atravessam todas as coleções do usuário:
 * volumes faltantes (história 11) e lista de interesse (histórias 13 e 14).
 *
 * Usa "collection group": busca em TODAS as subcoleções chamadas "volumes",
 * mas só dos documentos cujo ownerId é o usuário logado.
 */
public class ListaRepository {

    private static final String TAG = "MIPIM";

    public interface VolumesListener {
        void onChange(@NonNull List<Volume> volumes);
        void onError(@NonNull Exception e);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String uid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** História 11: todos os volumes ainda não adquiridos */
    public void observarFaltantes(Activity activity, VolumesListener listener) {
        observar(activity, "adquirido", false, listener);
    }

    /** Histórias 13 e 14: volumes na lista de interesse */
    public void observarDesejados(Activity activity, VolumesListener listener) {
        observar(activity, "desejado", true, listener);
    }

    /**
     * Histórias 15, 16 e 18: volumes já adquiridos (base dos cálculos de gastos).
     * Usa o MESMO índice da tela de Faltantes (ownerId + adquirido).
     */
    public void observarAdquiridos(Activity activity, VolumesListener listener) {
        observar(activity, "adquirido", true, listener);
    }

    private void observar(Activity activity, String campo, boolean valor,
                          VolumesListener listener) {
        String uid = uid();
        if (uid == null) {
            listener.onError(new IllegalStateException("Usuário não logado"));
            return;
        }
        db.collectionGroup("volumes")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo(campo, valor)
                .addSnapshotListener(activity, (snap, e) -> {
                    if (e != null) {
                        // Se faltar índice, a mensagem do Logcat traz o link para criá-lo
                        Log.e(TAG, "Falha na consulta de volumes", e);
                        listener.onError(e);
                        return;
                    }
                    if (snap != null) {
                        listener.onChange(snap.toObjects(Volume.class));
                    }
                });
    }

    /**
     * Adiciona ou remove um volume da lista de interesse e define a prioridade.
     * Como "desejado" é um campo do próprio volume, não existe item duplicado.
     */
    public void definirInteresse(Volume volume, boolean desejado, String prioridade,
                                 ColecaoRepository.Callback callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError(new IllegalStateException("Usuário não logado"));
            return;
        }
        db.collection("users").document(uid)
                .collection("colecoes").document(volume.getColecaoId())
                .collection("volumes").document(volume.getId())
                .update("desejado", desejado,
                        "prioridade", desejado ? prioridade : null)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public static String friendlyError(Throwable e) {
        if (e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode()
                == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
            return "Falta criar um índice no Firestore. Veja o link no Logcat.";
        }
        return ColecaoRepository.friendlyError(e);
    }
}

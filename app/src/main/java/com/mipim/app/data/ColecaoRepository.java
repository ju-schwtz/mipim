package com.mipim.app.data;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;
import com.mipim.app.model.Colecao;
import com.mipim.app.model.Volume;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Acesso às coleções do usuário logado.
 * Tudo é gravado em users/{uid}/colecoes, então cada coleção
 * fica vinculada ao usuário que está logado no momento.
 */
public class ColecaoRepository {

    /** Limite de volumes por obra (uma transação aceita no máximo 500 escritas) */
    public static final int MAX_VOLUMES = 300;

    public interface Callback {
        void onSuccess();
        void onError(@NonNull Exception e);
    }

    public interface ListListener {
        void onChange(@NonNull List<Colecao> colecoes);
        void onError(@NonNull Exception e);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String uid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private CollectionReference colecoes(String uid) {
        return db.collection("users").document(uid).collection("colecoes");
    }

    /**
     * História 3: escuta as coleções do usuário em tempo real.
     * O listener é preso à Activity e removido sozinho no onStop().
     */
    public void observar(Activity activity, ListListener listener) {
        String uid = uid();
        if (uid == null) {
            listener.onError(new IllegalStateException("Usuário não logado"));
            return;
        }
        colecoes(uid).orderBy("tituloBusca")
                .addSnapshotListener(activity, (snap, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (snap != null) {
                        listener.onChange(snap.toObjects(Colecao.class));
                    }
                });
    }

    /**
     * História 4: adiciona a obra e cria todos os volumes (não adquiridos).
     * O ID do documento é gerado a partir do título; se já existir,
     * a transação falha com ALREADY_EXISTS e nada é gravado.
     */
    public void adicionar(String titulo, String autor, String tipo,
                          int totalVolumes, Callback callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError(new IllegalStateException("Usuário não logado"));
            return;
        }

        String colecaoId = gerarId(titulo);
        DocumentReference ref = colecoes(uid).document(colecaoId);

        db.runTransaction(tx -> {
            // Todas as leituras vêm antes das escritas
            if (tx.get(ref).exists()) {
                throw new FirebaseFirestoreException("Coleção duplicada",
                        FirebaseFirestoreException.Code.ALREADY_EXISTS);
            }

            Colecao colecao = new Colecao();
            colecao.setOwnerId(uid);
            colecao.setTitulo(titulo);
            colecao.setTituloBusca(normalizar(titulo));
            colecao.setAutor(autor);
            colecao.setTipo(tipo);
            colecao.setTotalVolumes(totalVolumes);
            colecao.setVolumesAdquiridos(0);
            tx.set(ref, colecao);

            for (int i = 1; i <= totalVolumes; i++) {
                Volume v = new Volume();
                v.setOwnerId(uid);
                v.setColecaoId(colecaoId);
                v.setColecaoTitulo(titulo);
                v.setNumero(i);
                v.setAdquirido(false);
                v.setDesejado(false);
                // ID "001", "002"... mantém a ordem numérica
                String volumeId = String.format(Locale.US, "%03d", i);
                tx.set(ref.collection("volumes").document(volumeId), v);
            }
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess())
          .addOnFailureListener(callback::onError);
    }

    /** História 6: remove a coleção e todos os seus volumes de uma só vez */
    public void remover(String colecaoId, Callback callback) {
        String uid = uid();
        if (uid == null) {
            callback.onError(new IllegalStateException("Usuário não logado"));
            return;
        }

        DocumentReference ref = colecoes(uid).document(colecaoId);
        ref.collection("volumes").get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    snap.getDocuments().forEach(d -> batch.delete(d.getReference()));
                    batch.delete(ref);
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    // ---------- Utilitários ----------

    /** "Dragon Ball Z" -> "dragon ball z"; "Mangá" -> "manga" (sem acento) */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /** "Dragon Ball Z" -> "dragon-ball-z" (seguro para ser ID de documento) */
    static String gerarId(String titulo) {
        String id = normalizar(titulo).replaceAll("[^a-z0-9]+", "-");
        id = id.replaceAll("^-+|-+$", "");
        return id.isEmpty() ? "colecao" : id;
    }

    public static boolean isDuplicada(Throwable e) {
        return e instanceof FirebaseFirestoreException
                && ((FirebaseFirestoreException) e).getCode()
                == FirebaseFirestoreException.Code.ALREADY_EXISTS;
    }

    public static String friendlyError(Throwable e) {
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case ALREADY_EXISTS:
                    return "Você já tem essa coleção na biblioteca.";
                case PERMISSION_DENIED:
                    return "Sem permissão. Verifique as regras do Firestore.";
                case UNAVAILABLE:
                    return "Sem conexão com a internet.";
                default:
                    break;
            }
        }
        return "Ocorreu um erro. Tente novamente.";
    }
}

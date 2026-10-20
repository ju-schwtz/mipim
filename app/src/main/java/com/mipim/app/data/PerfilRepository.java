package com.mipim.app.data;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/** Perfil do usuário logado (documento users/{uid}) */
public class PerfilRepository {

    public interface PerfilListener {
        void onChange(String nome, String email);
        void onError(@NonNull Exception e);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Escuta o perfil em tempo real (nome vem do Firestore, e-mail do Auth) */
    public void observar(Activity activity, PerfilListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new IllegalStateException("Usuário não logado"));
            return;
        }
        db.collection("users").document(user.getUid())
                .addSnapshotListener(activity, (snap, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    String nome = snap != null ? snap.getString("name") : null;
                    listener.onChange(nome, user.getEmail());
                });
    }

    /**
     * História 21: salva o nome.
     * Usa set com merge: atualiza o campo e também recria o perfil caso ele
     * não exista (por exemplo, se o cadastro falhou ao gravar no Firestore).
     */
    public void atualizarNome(String nome, ColecaoRepository.Callback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Usuário não logado"));
            return;
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("uid", user.getUid());
        dados.put("email", user.getEmail());
        dados.put("name", nome);

        db.collection("users").document(user.getUid())
                .set(dados, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /** Envia ao e-mail do usuário um link para redefinir a senha */
    public void enviarRedefinicaoSenha(ColecaoRepository.Callback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onError(new IllegalStateException("Usuário não logado"));
            return;
        }
        auth.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}

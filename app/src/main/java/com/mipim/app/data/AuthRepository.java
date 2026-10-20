package com.mipim.app.data;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    public interface Callback {
        void onSuccess();
        void onError(@NonNull Exception e);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public String currentUserEmail() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
    }

    public void logout() {
        auth.signOut();
    }

    public void register(String name, String email, String password, Callback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        callback.onError(new Exception("Não foi possível obter o usuário."));
                        return;
                    }
                    String uid = result.getUser().getUid();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("createdAt", FieldValue.serverTimestamp());

                    db.collection("users").document(uid).set(userData)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void login(String email, String password, Callback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public static boolean isEmailAlreadyUsed(Throwable e) {
        return e instanceof FirebaseAuthUserCollisionException;
    }

    public static String friendlyError(Throwable e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Este e-mail já está cadastrado.";
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Senha fraca. Use pelo menos 6 caracteres.";
        } else if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            return "E-mail ou senha inválidos.";
        } else if (e instanceof FirebaseNetworkException) {
            return "Sem conexão com a internet.";
        } else {
            return "Ocorreu um erro. Tente novamente.";
        }
    }
}
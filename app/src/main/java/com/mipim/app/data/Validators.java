package com.mipim.app.data;

import android.util.Patterns;

public final class Validators {

    private Validators() { }

    public static boolean isValidEmail(String email) {
        return email != null
                && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
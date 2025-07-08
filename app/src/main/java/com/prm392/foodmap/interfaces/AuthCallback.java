package com.prm392.foodmap.interfaces;

import com.google.firebase.auth.FirebaseUser;

public interface AuthCallback {
    void onAuthSuccess(FirebaseUser user);
    void onAuthFailure(Exception e);
}

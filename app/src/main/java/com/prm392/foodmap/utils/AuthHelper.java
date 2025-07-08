package com.prm392.foodmap.utils;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cloudinary.api.exceptions.ApiException;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.prm392.foodmap.R;
import com.prm392.foodmap.interfaces.AuthCallback;

public class AuthHelper {
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Activity activity;

    public AuthHelper(Activity activity) {
        this.activity = activity;
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public Intent getSignInIntent() {
        return mGoogleSignInClient.getSignInIntent();
    }

    public void handleSignInResult(Task<GoogleSignInAccount> completedTask, AuthCallback callback) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken(), callback);
        } catch (Exception e) {
            callback.onAuthFailure(e);
        }
    }

    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        boolean isNewUser = false;
                        if (task.getResult() != null && task.getResult().getAdditionalUserInfo() != null) {
                            isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        }
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // Đã xác thực email, cho phép vào app
                                callback.onAuthSuccess(user);
                            } else if (isNewUser) {
                                // Lần đầu đăng nhập, gửi email xác thực
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        callback.onAuthFailure(
                                                new Exception("Vui lòng kiểm tra email và xác thực tài khoản trước khi sử dụng ứng dụng.")
                                        );
                                    } else {
                                        callback.onAuthFailure(
                                                new Exception("Không thể gửi email xác thực. Vui lòng thử lại.")
                                        );
                                    }
                                });
                            } else {
                                // User cũ, chưa xác thực email
                                callback.onAuthFailure(
                                        new Exception("Tài khoản chưa xác thực email. Vui lòng kiểm tra email để xác thực.")
                                );
                            }
                        }
                    } else {
                        callback.onAuthFailure(task.getException());
                    }
                });
    }
}

package com.prm392.foodmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.api.exceptions.ApiException;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private SignInButton btnGoogleSignIn;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        boolean isNewUser = false;
                        if (task.getResult() != null && task.getResult().getAdditionalUserInfo() != null) {
                            isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        }

                        if (user != null) {
                            if (!user.isEmailVerified()) {
                                // ✅ Gửi email xác thực nếu chưa xác thực, bất kể là user mới hay cũ
                                ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                                        .setUrl("https://prm392.page.link/EmailVerificationLink")
                                        .setHandleCodeInApp(true)
                                        .setAndroidPackageName("com.prm392.foodmap", true, null)
                                        .build();

                                user.sendEmailVerification(actionCodeSettings).addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        FirebaseDatabase.getInstance().getReference("users")
                                                .child(user.getUid())
                                                .child("isManuallyVerified")
                                                .setValue(false)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Đã gửi email xác thực.", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(this, VerifyEmailActivity.class));
                                                    finish();
                                                });
                                    } else {
                                        Toast.makeText(this, "Gửi email xác thực thất bại: " + verifyTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                // ✅ Email đã xác thực → kiểm tra trong DB
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                                        .child(user.getUid())
                                        .child("isManuallyVerified");

                                ref.get().addOnSuccessListener(snapshot -> {
                                    Boolean isVerified = snapshot.getValue(Boolean.class);
                                    if (Boolean.TRUE.equals(isVerified)) {
                                        goToMain();
                                    } else {
                                        startActivity(new Intent(this, VerifyEmailActivity.class));
                                        finish();
                                    }
                                });
                            }
                        }
                    } else {
                        Toast.makeText(this, "Đăng nhập thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

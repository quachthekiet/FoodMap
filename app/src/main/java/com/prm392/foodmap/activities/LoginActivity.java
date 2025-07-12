package com.prm392.foodmap.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.prm392.foodmap.R;
import com.prm392.foodmap.models.Constants;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private SignInButton btnGoogleSignIn;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;

    // ──────────────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ──────────────────────────────────────────────────────────────────────────────
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
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignIn.setOnClickListener(v ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser current = mAuth.getCurrentUser();
        if (current != null) checkUserRole(current);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // GOOGLE SIGN‑IN CALLBACK
    // ──────────────────────────────────────────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RC_SIGN_IN) return;

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // FIREBASE AUTH  +  LƯU USER
    // ──────────────────────────────────────────────────────────────────────────────
    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Toast.makeText(this, "Lỗi: idToken null", Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Đăng nhập Firebase thất bại", Toast.LENGTH_LONG).show();
                return;
            }
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_LONG).show();
                return;
            }
            saveUserIfFirstLogin(user);
        });
    }

    private void saveUserIfFirstLogin(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("email", user.getEmail());
                    data.put("name", user.getDisplayName());
                    data.put("role", Constants.ROLE_USER);
                    data.put("createdAt", System.currentTimeMillis());
                    userRef.setValue(data);
                }
                checkUserRole(user);   // luôn gọi để điều hướng
            }
            @Override public void onCancelled(@NonNull DatabaseError err) {
                Toast.makeText(LoginActivity.this, "Lỗi DB: " + err.getMessage(), Toast.LENGTH_LONG).show();
                goToMain();
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // ĐIỀU HƯỚNG THEO ROLE
    // ──────────────────────────────────────────────────────────────────────────────
    private void checkUserRole(FirebaseUser user) {
        DatabaseReference roleRef = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(user.getUid()).child("role");

        roleRef.get().addOnSuccessListener(snap -> {
            String role = snap.getValue(String.class);
            // Với mọi tài khoản (admin hay user), đều chuyển về HomeActivity
            startActivity(new Intent(this, HomeActivity.class));
            finish();

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi xác định vai trò", Toast.LENGTH_LONG).show();
            goToMain();
        });
    }

    private void goToMain() {
        setResult(RESULT_OK);
        finish();
    }
}

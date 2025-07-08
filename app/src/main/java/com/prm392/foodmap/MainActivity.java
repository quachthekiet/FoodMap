package com.prm392.foodmap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 👇 THÊM DÒNG NÀY
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        Uri deepLink = getIntent().getData();
        if (deepLink != null && deepLink.toString().contains("mode=verifyEmail")) {
            handleEmailVerificationDeepLink(deepLink);
        } else {
            checkManualVerification();
        }
    }

    private void handleEmailVerificationDeepLink(Uri deepLink) {
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Reload to get latest emailVerified status
        user.reload().addOnSuccessListener(aVoid -> {
            if (user.isEmailVerified()) {
                FirebaseDatabase.getInstance().getReference("users")
                        .child(user.getUid())
                        .child("isManuallyVerified")
                        .setValue(true)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                            goToMain();
                        });
            } else {
                Toast.makeText(this, "Bạn chưa xác thực email!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, VerifyEmailActivity.class));
                finish();
            }
        });
    }

    private void checkManualVerification() {
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Reload to get latest info
        user.reload().addOnSuccessListener(unused -> {
            if (user.isEmailVerified()) {
                FirebaseDatabase.getInstance().getReference("users")
                        .child(user.getUid())
                        .child("isManuallyVerified")
                        .get().addOnSuccessListener(snapshot -> {
                            Boolean isVerified = snapshot.getValue(Boolean.class);
                            if (Boolean.TRUE.equals(isVerified)) {
                                // Đã xác thực → ở lại Main
                            } else {
                                // Đã xác thực email nhưng chưa cập nhật flag → cập nhật
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(user.getUid())
                                        .child("isManuallyVerified")
                                        .setValue(true);
                            }
                        });
            } else {
                startActivity(new Intent(this, VerifyEmailActivity.class));
                finish();
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

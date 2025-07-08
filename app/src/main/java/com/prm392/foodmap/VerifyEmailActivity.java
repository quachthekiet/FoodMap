package com.prm392.foodmap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView txtMessage;
    private Button btnResend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        txtMessage = findViewById(R.id.txtVerifyMessage);
        btnResend = findViewById(R.id.btnResendEmail);

        btnResend.setOnClickListener(v -> resendVerification());
    }

    private void resendVerification() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Đã gửi email xác thực.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Không thể gửi lại email xác thực.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}

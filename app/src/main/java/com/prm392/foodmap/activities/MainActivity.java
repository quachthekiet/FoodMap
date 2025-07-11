package com.prm392.foodmap.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prm392.foodmap.R;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            // Chưa đăng nhập, chuyển về LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            // Đã đăng nhập, bỏ qua xác thực email, ở lại MainActivity
            // Bạn có thể load dữ liệu hoặc UI ở đây

            // Ví dụ: chuyển sang RestaurantActivity
            onpenRestaurantActivity();
        }
    }

    private void onpenRestaurantActivity() {
        Intent intent = new Intent(this, RestaurantActivity.class);
        intent.putExtra("RESTAURANT_ID", "restaurant001");
        startActivity(intent);
    }
}

package com.prm392.foodmap.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.R;
import com.prm392.foodmap.activities.AdminActivity;
import com.prm392.foodmap.activities.AddRestaurantActivity;
import com.prm392.foodmap.models.Constants;

import java.util.Objects;

public class ProfileFragment extends Fragment {

    private TextView tvEmail, tvName;
    private Button btnAuth;
    private Button btnManage;
    private Button btnMyRestaurant;
    private Button btnAddRestaurant;
    private Button btnVerifyRestaurant; // 👈 Nút mới

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleClient;

    private OnAuthButtonClickListener authCallback;

    public interface OnAuthButtonClickListener {
        void onAuthButtonClicked();
        void onLogout();
        void onLogoutSuccess();
    }

    public ProfileFragment() { }

    public void setGoogleSignInClient(GoogleSignInClient client) {
        this.googleClient = client;
    }

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof OnAuthButtonClickListener) {
            authCallback = (OnAuthButtonClickListener) ctx;
        } else {
            throw new RuntimeException(ctx + " must implement OnAuthButtonClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.profile_fragment, parent, false);

        tvEmail = v.findViewById(R.id.tvEmail);
        tvName = v.findViewById(R.id.tvName);
        btnAuth = v.findViewById(R.id.btnLogout);
        btnManage = v.findViewById(R.id.btnManage);
        btnAddRestaurant = v.findViewById(R.id.btnAddRestaurant);
        btnMyRestaurant = v.findViewById(R.id.btnMyRestaurant);
        btnVerifyRestaurant = v.findViewById(R.id.btnVerifyRestaurant);

        mAuth = FirebaseAuth.getInstance();
        updateUI(mAuth.getCurrentUser());

        btnAddRestaurant.setOnClickListener(view -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm nhà hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(getContext(), AddRestaurantActivity.class));
        });

        btnMyRestaurant.setOnClickListener(view -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để xem nhà hàng của tôi", Toast.LENGTH_SHORT).show();
                return;
            }
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.profileDrawer, new MyRestaurantFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnManage.setVisibility(View.GONE);
        btnVerifyRestaurant.setVisibility(View.GONE); // 👈 Mặc định ẩn

        btnManage.setOnClickListener(view ->
                startActivity(new Intent(getContext(), AdminActivity.class)));

        btnVerifyRestaurant.setOnClickListener(view -> {
            Toast.makeText(getContext(), "Chức năng xác minh nhà hàng sẽ được cập nhật sau!", Toast.LENGTH_SHORT).show();
            // startActivity(new Intent(getContext(), VerifyRestaurantActivity.class));
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkIfAdminAndShowManage();
    }

    public void updateUI(FirebaseUser user) {
        if (user == null) {
            tvEmail.setText("");
            tvName.setText("");
            btnAuth.setText("Đăng nhập Google");
            btnAuth.setOnClickListener(v -> {
                if (authCallback != null) authCallback.onAuthButtonClicked();
            });
            btnManage.setVisibility(View.GONE);
            btnVerifyRestaurant.setVisibility(View.GONE); // 👈 Ẩn nếu chưa đăng nhập
            return;
        }

        tvEmail.setText("Email: " + user.getEmail());
        tvName.setText("Tên: " + (user.getDisplayName() == null ? "Không có tên" : user.getDisplayName()));
        btnAuth.setText("Đăng xuất");
        btnAuth.setOnClickListener(v -> {
            if (authCallback != null) authCallback.onLogout();
            mAuth.signOut();
            if (googleClient != null) {
                googleClient.revokeAccess().addOnCompleteListener(t -> {
                    if (authCallback != null) authCallback.onLogoutSuccess();
                    updateUI(null);
                });
            } else {
                if (authCallback != null) authCallback.onLogoutSuccess();
                updateUI(null);
            }
        });

        checkIfAdminAndShowManage();
    }

    private void checkIfAdminAndShowManage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DatabaseReference roleRef = FirebaseDatabase
                .getInstance("https://food-map-app-2025-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users")
                .child(Objects.requireNonNull(user.getUid()))
                .child("role");

        roleRef.get().addOnSuccessListener(snap -> {
            String role = snap.getValue(String.class);
            boolean isAdmin = Constants.ROLE_ADMIN.equals(role)
                    || Constants.ROLE_SYSTEM_ADMIN.equals(role);

            btnManage.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            btnVerifyRestaurant.setVisibility(isAdmin ? View.VISIBLE : View.GONE);

            if (isAdmin) {
                btnVerifyRestaurant.setOnClickListener(view -> {
                    // TODO: Replace with actual logic to open verification screen
                    startActivity(new Intent(getContext(), AdminActivity.class)
                            .putExtra("mode", "verify"));
                });
            }
        });
    }
}

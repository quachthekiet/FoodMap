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

/**
 * Hiển thị thông tin người dùng, Logout, và (nếu là admin) nút Quản lý.
 */
public class ProfileFragment extends Fragment {

    // UI -------------------------------------------------------------------
    private TextView tvEmail, tvName;
    private Button   btnAuth;      // Đăng nhập / Đăng xuất
    private Button   btnManage;    // Quản lý (chỉ admin mới thấy)
    private Button btnMyRestaurant;

    private Button btnAddRestaurant;

    // Firebase --------------------------------------------------------------
    private FirebaseAuth        mAuth;
    private GoogleSignInClient  googleClient;

    // callback về Activity --------------------------------------------------
    private OnAuthButtonClickListener authCallback;

    public interface OnAuthButtonClickListener {
        void onAuthButtonClicked();  // khi user bấm “Đăng nhập”
        void onLogout();             // trước khi logout
        void onLogoutSuccess();      // sau khi logout thành công
    }

    // bắt buộc phải có constructor rỗng
    public ProfileFragment() { }

    // Cho phép HomeActivity truyền GoogleSignInClient vào
    public void setGoogleSignInClient(GoogleSignInClient client) {
        this.googleClient = client;
    }

    // ----------------------------------------------------------------------
    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof OnAuthButtonClickListener) {
            authCallback = (OnAuthButtonClickListener) ctx;
        } else {
            throw new RuntimeException(ctx + " must implement OnAuthButtonClickListener");
        }
    }

    // ----------------------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.profile_fragment, parent, false);

        // bind view
        tvEmail   = v.findViewById(R.id.tvEmail);
        tvName    = v.findViewById(R.id.tvName);
        btnAuth   = v.findViewById(R.id.btnLogout);   // nút đã có sẵn trong layout
        btnManage = v.findViewById(R.id.btnManage);   // bạn vừa thêm trong XML
        btnAddRestaurant = v.findViewById(R.id.btnAddRestaurant);
        btnMyRestaurant = v.findViewById(R.id.btnMyRestaurant);
        mAuth = FirebaseAuth.getInstance();
        updateUI(mAuth.getCurrentUser());
        btnAddRestaurant.setOnClickListener(view -> {
            // Kiểm tra đăng nhập
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm nhà hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(getContext(), AddRestaurantActivity.class));
        });
        btnMyRestaurant.setOnClickListener(view -> {
            // Kiểm tra đăng nhập
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để xem My Restaurant", Toast.LENGTH_SHORT).show();
                return;
            }
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.profileDrawer, new MyRestaurantFragment()) // ID của container trong Activity
                    .addToBackStack(null)
                    .commit();
        });                       
        // “Quản lý” – mặc định ẩn, chỉ hiển thị nếu là admin
        btnManage.setVisibility(View.GONE);
        btnManage.setOnClickListener(view ->
                startActivity(new Intent(getContext(), AdminActivity.class)));

        return v;
    }

    // ----------------------------------------------------------------------
    @Override
    public void onResume() {
        super.onResume();
        checkIfAdminAndShowManage();      // luôn kiểm tra khi fragment hiển thị lại
    }

    // ----------------------------------------------------------------------
    /** Cập nhật giao diện tùy theo trạng thái đăng nhập */
    public void updateUI(FirebaseUser user) {
        if (user == null) {                                   // CHƯA đăng nhập
            tvEmail.setText("");
            tvName .setText("");
            btnAuth.setText("Đăng nhập Google");
            btnAuth.setOnClickListener(v -> {
                if (authCallback != null) authCallback.onAuthButtonClicked();
            });
            btnManage.setVisibility(View.GONE);               // ẩn nút quản lý
            return;
        }

        // Đã login ----------------------------------------------------------
        tvEmail.setText("Email: " + user.getEmail());
        tvName .setText("Tên: "   + (user.getDisplayName() == null
                ? "Không có tên" : user.getDisplayName()));
        btnAuth.setText("Đăng xuất");
        btnAuth.setOnClickListener(v -> {
            if (authCallback != null) authCallback.onLogout();    // thông báo trước
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

        // Sau khi đã có user → kiểm tra role
        checkIfAdminAndShowManage();
    }

    // ----------------------------------------------------------------------
    /** Kiểm tra role và show/hide nút “Quản lý” */
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
        });
    }
}

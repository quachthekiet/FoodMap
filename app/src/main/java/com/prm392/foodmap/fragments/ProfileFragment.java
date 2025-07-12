package com.prm392.foodmap.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prm392.foodmap.R;

public class ProfileFragment extends Fragment {

    private TextView tvEmail, tvName;
    private Button btnAuth;
    private FirebaseAuth mAuth;

    private Button btnLogo;

    private OnAuthButtonClickListener authButtonClickListener;
    private GoogleSignInClient mGoogleSignInClient;

    public interface OnAuthButtonClickListener {
        void onAuthButtonClicked();
        void onLogout();
        void onLogoutSuccess();
    }

    // Constructor mặc định bắt buộc phải có
    public ProfileFragment() {
        // Required empty public constructor
    }

    // Setter để truyền GoogleSignInClient từ bên ngoài
    public void setGoogleSignInClient(GoogleSignInClient googleSignInClient) {
        this.mGoogleSignInClient = googleSignInClient;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAuthButtonClickListener) {
            authButtonClickListener = (OnAuthButtonClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnAuthButtonClickListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        tvEmail = view.findViewById(R.id.tvEmail);
        tvName = view.findViewById(R.id.tvName);
        btnAuth = view.findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        updateUI(mAuth.getCurrentUser());

        return view;
    }

    public void updateUI(FirebaseUser user) {
        if (user != null) {
            tvEmail.setText("Email: " + user.getEmail());
            tvName.setText("Tên: " + (user.getDisplayName() != null ? user.getDisplayName() : "Không có tên"));
            btnAuth.setText("Đăng xuất");
            btnAuth.setOnClickListener(v -> {
                mAuth.signOut(); // Đăng xuất Firebase
                if (mGoogleSignInClient != null) {
                    // Thu hồi quyền truy cập Google để buộc chọn tài khoản lại
                    mGoogleSignInClient.revokeAccess().addOnCompleteListener(task -> {
                        if (authButtonClickListener != null) {
                            authButtonClickListener.onLogout();
                            authButtonClickListener.onLogoutSuccess();
                        }
                        updateUI(null); // Cập nhật UI sau khi đăng xuất
                    });
                } else {
                    if (authButtonClickListener != null) {
                        authButtonClickListener.onLogout();
                        authButtonClickListener.onLogoutSuccess();
                    }
                    updateUI(null);
                }
            });
        } else {
            tvEmail.setText("");
            tvName.setText("");
            btnAuth.setText("Đăng nhập Google");
            btnAuth.setOnClickListener(v -> {
                if (authButtonClickListener != null) {
                    authButtonClickListener.onAuthButtonClicked();
                }
            });
        }
    }
}

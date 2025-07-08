package com.prm392.foodmap.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.prm392.foodmap.R;
import com.prm392.foodmap.utils.FirebaseHelper;

public class UserProfileFragment extends Fragment {
    private void updateProfile(String newName, String newRole) {
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseHelper.updateUserProfile(
                userUid,
                newName,
                newRole,
                (error, ref) -> {
                    if (error == null) {
                        // Thành công
                    } else {
                        // Lỗi
                    }
                }
        );
    }
}
package com.prm392.foodmap.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prm392.foodmap.R;
import com.prm392.foodmap.utils.FirebaseHelper;

// ... import các package cần thiết

public class RestaurantListFragment extends Fragment {
    // ... các biến khác

    private void setRestaurantVisible(String restaurantId, boolean visible) {
        FirebaseHelper.setRestaurantVisible(restaurantId, visible, (error, ref) -> {
            if (error == null) {
                // Thành công
            } else {
                // Lỗi
            }
        });
    }
}

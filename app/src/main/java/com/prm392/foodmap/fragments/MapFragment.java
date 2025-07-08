package com.prm392.foodmap.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prm392.foodmap.R;
import com.prm392.foodmap.utils.LocationHelper;

// ... import các package cần thiết

public class MapFragment extends Fragment {
    // ... các biến khác

    private void tryPinLocation() {
        if (LocationHelper.canUserPinLocation()) {
            // Cho phép pin vị trí
        } else {
            // Hiển thị thông báo yêu cầu đăng nhập
        }
    }
}

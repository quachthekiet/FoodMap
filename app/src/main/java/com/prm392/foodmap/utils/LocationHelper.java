package com.prm392.foodmap.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LocationHelper {
    public static boolean canUserPinLocation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null; // Có thể kiểm tra thêm verified hoặc role nếu muốn
    }
}

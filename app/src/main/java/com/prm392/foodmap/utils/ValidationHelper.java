package com.prm392.foodmap.utils;

import android.text.TextUtils;
import java.util.List;

public class ValidationHelper {
    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name.trim());
    }

    public static boolean isValidPhone(String phone) {
        return !TextUtils.isEmpty(phone.trim()) && phone.matches("^\\+?[0-9]{9,15}$");
    }

    public static boolean isValidLatLng(Double lat, Double lng) {
        return lat != null && lng != null;
    }

    public static boolean isValidAddForm(String name, String phone, Double lat, Double lng,
                                         List<String> imageUrls) {
        return isValidName(name)
                && isValidPhone(phone)
                && isValidLatLng(lat, lng)
                && imageUrls != null && !imageUrls.isEmpty();
    }

    public static boolean isValidUpdateForm(String name, String phone, Double lat, Double lng,
                                            List<String> imageUrls, List<String> menuImageUrls) {
        return isValidName(name)
                && isValidPhone(phone)
                && isValidLatLng(lat, lng)
                && imageUrls != null && !imageUrls.isEmpty()
                && menuImageUrls != null && !menuImageUrls.isEmpty();
    }
}

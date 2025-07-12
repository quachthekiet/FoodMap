package com.prm392.foodmap.utils;

import android.text.TextUtils;

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

    public static boolean isValidForm(String name, String phone, Double lat, Double lng) {
        return isValidName(name) && isValidPhone(phone) && isValidLatLng(lat, lng);
    }
}

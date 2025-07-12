package com.prm392.foodmap.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

public class LocationUtil {
    private static final String PREFS_NAME = "location_cache";
    private static final String KEY_LAT = "cached_lat";
    private static final String KEY_LNG = "cached_lng";

    public static void saveLocation(Context context, double lat, double lng) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_LNG, Double.doubleToRawLongBits(lng))
                .apply();
    }

    public static LatLng getSavedLocation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LNG)) return null;

        double lat = Double.longBitsToDouble(prefs.getLong(KEY_LAT, 0));
        double lng = Double.longBitsToDouble(prefs.getLong(KEY_LNG, 0));
        return new LatLng(lat, lng);
    }
}

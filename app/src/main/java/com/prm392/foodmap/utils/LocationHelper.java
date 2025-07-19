package com.prm392.foodmap.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.prm392.foodmap.interfaces.DataCallback;

public class LocationHelper {
    @SuppressLint("MissingPermission")
    public static void getLastKnownLocation(Context context, OnSuccessListener<Location> listener) {
        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation().addOnSuccessListener(listener);
    }

    public static boolean canUserPinLocation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null;
    }
    public static void checkGPSDistance(Context context, double resLat, double resLng, DataCallback<Boolean> callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2000);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                float[] result = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        resLat, resLng, result
                );
                float distance = result[0];
                callback.onSuccess(distance <= 100);
            } else {
                callback.onError("Không thể lấy vị trí hiện tại!");
            }
        });
    }
}

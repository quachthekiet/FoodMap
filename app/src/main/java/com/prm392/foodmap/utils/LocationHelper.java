package com.prm392.foodmap.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHelper {
    @SuppressLint("MissingPermission")
    public static void getLastKnownLocation(Context context, OnSuccessListener<Location> listener) {
        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation().addOnSuccessListener(listener);
    }
}

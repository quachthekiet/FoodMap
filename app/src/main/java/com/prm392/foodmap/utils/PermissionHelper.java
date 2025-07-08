package com.prm392.foodmap.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    public static final int REQUEST_LOCATION_PERMISSION = 1001;

    public static boolean hasFineLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestFineLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION
        );
    }

    public static boolean handleLocationPermissionResult(int requestCode, int[] grantResults) {
        return requestCode == REQUEST_LOCATION_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}

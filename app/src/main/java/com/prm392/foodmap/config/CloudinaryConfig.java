package com.prm392.foodmap.config;

import android.content.Context;

import com.cloudinary.android.MediaManager;
import com.prm392.foodmap.R;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static boolean isInitialized = false;
    public static void setupCloudinary(Context context) {
        if (!isInitialized) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "df5rjcqoh");
        config.put("api_key", "854178415444652");
        String apiSecret = context.getString(R.string.cloudinary_api_secret);
        config.put("api_secret", apiSecret);
        MediaManager.init(context.getApplicationContext(), config);
        isInitialized=true;
        }
    }

}

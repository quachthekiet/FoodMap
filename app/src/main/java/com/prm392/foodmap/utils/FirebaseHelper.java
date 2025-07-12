package com.prm392.foodmap.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper {
    // Enable/Disable nhà hàng (pin)
    public static void setRestaurantVisible(String restaurantId, boolean visible, DatabaseReference.CompletionListener listener) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("restaurants").child(restaurantId).child("isVisible");
        ref.setValue(visible, listener);
    }
}
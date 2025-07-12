package com.prm392.foodmap.utils;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.prm392.foodmap.models.Restaurant;
import com.prm392.foodmap.models.RestaurantWithKey;
import com.prm392.foodmap.interfaces.DataCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    // Enable/Disable nhà hàng (pin)
    public static void setRestaurantVisible(String restaurantId, boolean visible, DatabaseReference.CompletionListener listener) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("restaurants").child(restaurantId).child("isVisible");
        ref.setValue(visible, listener);
    }
    public static void loadAllRestaurants(DataCallback<List<RestaurantWithKey>> callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("restaurants");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RestaurantWithKey> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Restaurant r = ds.getValue(Restaurant.class);
                    if (r != null) {
                        list.add(new RestaurantWithKey(ds.getKey(), r));
                    }
                }
                callback.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Lỗi tải nhà hàng: " + error.getMessage());
            }
        });
    }
    public static void loadAllVisibleRestaurants(DataCallback<List<RestaurantWithKey>> callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("restaurants");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<RestaurantWithKey> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Restaurant r = ds.getValue(Restaurant.class);
                    if (r != null && r.isVisible()) {
                        list.add(new RestaurantWithKey(ds.getKey(), r));
                    }
                }
                callback.onSuccess(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Lỗi tải nhà hàng: " + error.getMessage());
            }
        });
    }
    public static void loadRestaurantsByOwner(String ownerUid, DataCallback<List<RestaurantWithKey>> callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("restaurants");
        ref.orderByChild("ownerUid").equalTo(ownerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<RestaurantWithKey> list = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Restaurant r = ds.getValue(Restaurant.class);
                            if (r != null) {
                                list.add(new RestaurantWithKey(ds.getKey(), r));
                            }
                        }
                        callback.onSuccess(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError("Lỗi tải nhà hàng của bạn: " + error.getMessage());
                    }
                });
    }
    public static void addRestaurant(Map<String, Object> restaurantData, DataCallback<Void> callback) {
        DatabaseReference restaurantsRef = FirebaseDatabase.getInstance()
                .getReference("restaurants");
        String newRestaurantId = restaurantsRef.push().getKey();

        if (newRestaurantId == null) {
            if (callback != null) {
                callback.onError("Lỗi tạo ID quán ăn");
            }
            return;
        }

        restaurantsRef.child(newRestaurantId)
                .setValue(restaurantData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public static void updateRestaurant(String key, Restaurant restaurant, DataCallback<Void> callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(key);

        ref.setValue(restaurant)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    public static void getRestaurantByKey(String key, DataCallback<Restaurant> callback) {
        if (key == null || key.isEmpty()) {
            if (callback != null) {
                callback.onError("Restaurant key is null or empty.");
            }
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(key);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Restaurant restaurant = snapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        // Tùy chọn: Gán key vào đối tượng Restaurant nếu bạn có trường transient key trong model
                        // restaurant.setKey(key);
                        if (callback != null) {
                            callback.onSuccess(restaurant);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Failed to parse restaurant data.");
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Restaurant not found.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) {
                    callback.onError(error.getMessage());
                }
            }
        });
    }

}
package com.prm392.foodmap.models;

public class RestaurantWithKey {
    public String key;
    public Restaurant restaurant;

    public RestaurantWithKey() {}

    public RestaurantWithKey(String key, Restaurant restaurant) {
        this.key = key;
        this.restaurant = restaurant;
        // Set key vào restaurant object để tiện sử dụng
        if (restaurant != null) {
            restaurant.setKey(key);
        }
    }

    public String getKey() {
        return key;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }
}

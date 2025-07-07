package com.prm392.foodmap.models;

public class ReviewWithInfo {
    public String deviceId;
    public String restaurantId;
    public Review review;

    public ReviewWithInfo() {}

    public ReviewWithInfo(String deviceId, String restaurantId, Review review) {
        this.deviceId = deviceId;
        this.restaurantId = restaurantId;
        this.review = review;
        // Set thông tin vào review object
        if (review != null) {
            review.setDeviceId(deviceId);
            review.setRestaurantId(restaurantId);
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public Review getReview() {
        return review;
    }
}

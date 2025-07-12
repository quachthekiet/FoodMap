package com.prm392.foodmap.models;

public class Review {
    public int rating;
    public String comment;
    public long timestamp;

    // Transient fields
    public transient String deviceId;
    public transient String restaurantId;

    // Constructor không tham số - BẮT BUỘC cho Firebase
    public Review() {}

    // Constructor với tham số
    public Review(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor đầy đủ
    public Review(int rating, String comment, long timestamp) {
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    // Validation methods
    public boolean isValidRating() {
        return rating >= 1 && rating <= 5;
    }

    public boolean isValidComment() {
        return comment != null && !comment.trim().isEmpty();
    }

    public boolean isValid() {
        return isValidRating() && isValidComment();
    }

    // Helper methods
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getFormattedTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }

    @Override
    public String toString() {
        return "Review{" +
                "rating=" + rating +
                ", comment='" + comment + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }
}

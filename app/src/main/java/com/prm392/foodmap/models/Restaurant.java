package com.prm392.foodmap.models;

import java.util.HashMap;
import java.util.Map;

public class Restaurant {
    public String name;
    public String address;
    public String phone;
    public String ownerUid;
    public double latitude;
    public double longitude;
    public Map<String, String> images;
    public Map<String, String> menuImages;
    public boolean isVisible;
    public long createdAt;
    public long updatedAt;

    public transient float averageRating = 0;
    public transient int reviewCount = 0;

    public transient float distance;

    // Transient field - không lưu vào Firebase
    public transient String key;

    // Constructor không tham số - BẮT BUỘC cho Firebase
    public Restaurant() {
        images = new HashMap<>();
        menuImages = new HashMap<>();
    }

    // Constructor với tham số
    public Restaurant(String name, String address, double latitude, double longitude,
                      String phone, String ownerUid) {
        this();
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.ownerUid = ownerUid;
        this.isVisible = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Helper methods
    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void addImage(String imageId, String imageUrl) {
        if (images == null) {
            images = new HashMap<>();
        }
        images.put(imageId, imageUrl);
    }

    public void addMenuImage(String menuId, String menuUrl) {
        if (menuImages == null) {
            menuImages = new HashMap<>();
        }
        menuImages.put(menuId, menuUrl);
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }
}

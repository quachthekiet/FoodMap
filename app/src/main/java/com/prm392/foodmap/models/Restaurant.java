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
    public boolean isVisible(){
        return isVisible;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Map<String, String> getImages() {
        return images;
    }

    public void setImages(Map<String, String> images) {
        this.images = images;
    }

    public Map<String, String> getMenuImages() {
        return menuImages;
    }

    public void setMenuImages(Map<String, String> menuImages) {
        this.menuImages = menuImages;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

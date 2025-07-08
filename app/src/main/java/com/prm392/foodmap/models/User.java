package com.prm392.foodmap.models;

public class User {
    public String email;
    public String name;
    public String role;
    public long createdAt;
    public boolean isManuallyVerified;

    public transient String uid;

    // Constructor mặc định (bắt buộc)
    public User() {}

    // Constructor có tham số
    public User(String email, String name, String role) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
        this.isManuallyVerified = false;
    }

    // Helper methods để kiểm tra role
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    public boolean isUser() {
        return Constants.ROLE_RESTAURANT_OWNER.equals(role);
    }

    public boolean isSystemAdmin() {
        return Constants.ROLE_SYSTEM_ADMIN.equals(role);
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public String getRole() {
        return role;
    }
}
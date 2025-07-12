package com.prm392.foodmap.models;

public class User {
    public String email;
    public String name;
    public String role;
    public long createdAt;

    // Transient field
    public transient String uid;

    // Constructor không tham số - BẮT BUỘC cho Firebase
    public User() {}

    // Constructor với tham số
    public User(String email, String name, String role) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // Helper methods để kiểm tra role
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    public boolean isUser() {
        return Constants.ROLE_USER.equals(role);
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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

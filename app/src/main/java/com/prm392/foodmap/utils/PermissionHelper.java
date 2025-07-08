package com.prm392.foodmap.utils;

import com.prm392.foodmap.models.User;

public class PermissionHelper {
    public static boolean isAdmin(User user) {
        return user != null && "admin".equals(user.getRole());
    }
}

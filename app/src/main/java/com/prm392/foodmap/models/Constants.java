package com.prm392.foodmap.models;

public class Constants {
    // Firebase nodes
    public static final String NODE_RESTAURANTS = "restaurants";
    public static final String NODE_USERS = "users";
    public static final String NODE_REVIEWS = "reviews";

    // User roles
    public static final String ROLE_SYSTEM_ADMIN = "system_admin";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    // Review constraints
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MIN_COMMENT_LENGTH = 5;
    public static final int MAX_COMMENT_LENGTH = 500;

    // Image constraints
    public static final int MAX_IMAGES_PER_RESTAURANT = 10;
    public static final int MAX_MENU_IMAGES_PER_RESTAURANT = 5;
    public static final long MAX_IMAGE_SIZE_MB = 5;

    private Constants() {
        // Prevent instantiation
    }
}

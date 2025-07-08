package com.prm392.foodmap.interfaces;

public interface OtpCallback {
    void onOtpVerified();
    void onOtpFailed(String message);
}
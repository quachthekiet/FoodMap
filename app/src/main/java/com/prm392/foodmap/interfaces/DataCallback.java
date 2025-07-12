package com.prm392.foodmap.interfaces;

public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(String errorMessage);
}

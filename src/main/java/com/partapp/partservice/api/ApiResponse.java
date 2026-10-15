package com.partapp.partservice.api;

public record ApiResponse<T>(int code, String message, T data, String requestId) {
    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(0, "ok", data, requestId);
    }
}

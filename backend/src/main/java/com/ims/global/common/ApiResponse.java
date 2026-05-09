package com.ims.global.common;

import com.ims.global.exception.ErrorCode;

public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(message, null);
    }
}

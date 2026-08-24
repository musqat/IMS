package com.ims.global.common;

import com.ims.global.exception.ErrorCode;

/**
 * 공통 응답 래퍼
 *
 * code는 실패 응답에만 실린다. ErrorCode의 enum 이름을 그대로 쓴다.
 * HTTP status만으로는 구분이 안 되기 때문이다
 * 프론트는 code로 분기하고 message는 그대로 표시한다.
 * 메시지 문구를 고쳐도 분기가 깨지지 않는다.
 */
public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(null, "success", data);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.name(), errorCode.getMessage(), null);
    }

    /**
     * ErrorCode가 없는 실패 (검증 오류, 잘못된 JSON 등)
     */
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(null, message, null);
    }
}

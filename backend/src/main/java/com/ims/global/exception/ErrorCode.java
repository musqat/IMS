package com.ims.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // Authorization
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    WAREHOUSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "담당 창고에만 접근할 수 있습니다."),

    // User
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // SubUser
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 ID입니다."),
    SUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 하위 계정입니다."),

    // Business
    CANCEL_DEADLINE_EXCEEDED(HttpStatus.BAD_REQUEST, "취소 가능 시간이 초과되었습니다."),
    ALREADY_SETTLED(HttpStatus.BAD_REQUEST, "이미 결산된 기록입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),

    // Resource
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),

    // Server
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}

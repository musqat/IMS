package com.ims.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // global
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // user
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // partnership
    INVALID_INVITE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 토큰입니다."),
    ALREADY_ACCEPTED(HttpStatus.CONFLICT, "이미 수락된 초대입니다."),
    DUPLICATE_PARTNERSHIP(HttpStatus.CONFLICT, "이미 존재하는 파트너십입니다."),
    SELF_INVITE(HttpStatus.BAD_REQUEST, "자기 자신을 초대할 수 없습니다."),
    NOT_PARTNER(HttpStatus.FORBIDDEN, "파트너 관계인 회사에게만 창고를 공유할 수 있습니다."),

    // warehouse
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 창고입니다."),
    WAREHOUSE_NOT_OWNED(HttpStatus.FORBIDDEN, "소유자와 창고가 맞지 않습니다."),

    // item
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 품목입니다."),
    DUPLICATE_ITEM_CODE(HttpStatus.CONFLICT, "이미 사용 중인 품목 코드입니다."),
    ITEM_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 품목에 대한 권한이 없습니다."),
    ITEM_IN_USE_BY_BOM(HttpStatus.CONFLICT, "BOM에 등록된 품목은 삭제할 수 없습니다."),

    // bom
    BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 BOM입니다."),
    DUPLICATE_BOM(HttpStatus.CONFLICT, "이미 등록된 BOM 관계입니다."),
    BOM_CIRCULAR_REFERENCE(HttpStatus.BAD_REQUEST, "BOM 순환 참조가 발생합니다."),
    BOM_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "자기 자신을 하위 품목으로 등록할 수 없습니다."),

    // inventory
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 재고 항목입니다."),
    DUPLICATE_INVENTORY(HttpStatus.CONFLICT, "해당 창고에 이미 등록된 품목입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다.");

    private final HttpStatus status;
    private final String message;
}

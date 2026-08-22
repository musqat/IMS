package com.ims.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // global
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // user
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다."),

    // partnership
    INVALID_INVITE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 토큰입니다."),
    ALREADY_ACCEPTED(HttpStatus.CONFLICT, "이미 수락된 초대입니다."),
    DUPLICATE_PARTNERSHIP(HttpStatus.CONFLICT, "이미 존재하는 파트너십입니다."),
    SELF_INVITE(HttpStatus.BAD_REQUEST, "자기 자신을 초대할 수 없습니다."),
    NOT_PARTNER(HttpStatus.FORBIDDEN, "파트너 관계인 회사에게만 창고를 공유할 수 있습니다."),
    PARTNERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파트너십입니다."),
    PARTNERSHIP_NOT_ACCEPTED(HttpStatus.BAD_REQUEST, "수락된 파트너십에만 가능한 작업입니다."),

    // warehouse
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 창고입니다."),
    WAREHOUSE_NOT_OWNED(HttpStatus.FORBIDDEN, "해당 창고를 소유하고 있지 않습니다."),
    WAREHOUSE_SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 창고 공유입니다."),
    WAREHOUSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 창고에 대한 접근 권한이 없습니다."),
    WAREHOUSE_HAS_INVENTORY(HttpStatus.CONFLICT, "재고가 남아 있어 창고를 삭제할 수 없습니다."),
    WAREHOUSE_HAS_PRODUCTION(HttpStatus.CONFLICT, "생산 기록이 있어 창고를 삭제할 수 없습니다."),

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
    BOM_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "BOM 구조가 허용 깊이(20단계)를 초과하였습니다."),

    // inventory
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 재고 항목입니다."),
    DUPLICATE_INVENTORY(HttpStatus.CONFLICT, "해당 창고에 이미 등록된 품목입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 0 이상이어야 합니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일이 종료일보다 클 수 없습니다."),
    DATE_RANGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "조회 기간은 최대 1년입니다."),

    // production
    PRODUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 생산 기록입니다."),
    PRODUCTION_NOT_CANCELLABLE(HttpStatus.BAD_REQUEST, "취소할 수 없는 생산 기록입니다."),
    PRODUCTION_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "PENDING 상태의 생산 기록만 수정할 수 있습니다."),
    PRODUCTION_ALREADY_SETTLED(HttpStatus.CONFLICT, "이미 결산된 생산 기록입니다."),
    PRODUCTION_NOT_SETTLED(HttpStatus.BAD_REQUEST, "결산이 완료된 생산 기록이 아닙니다."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결산 기록입니다.");

    private final HttpStatus status;
    private final String message;
}

package com.ims.user.dto.request;

/**
 * 비밀번호 정책
 *
 * 가입과 변경 두 곳에서 쓴다.
 * 애노테이션 속성은 상수여야 해서 문자열 상수로 둔다.
 */
public final class PasswordPolicy {

    /**
     * 영문과 숫자를 각각 하나 이상 포함해야 한다.
     * 특수문자는 강제하지 않는다 — 길이와 혼용만으로도 흔한 비밀번호는 걸러진다.
     */
    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    public static final String MESSAGE = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다";

    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }
}

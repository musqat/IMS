package com.ims.global.common;

/**
 * 의존성 상태 확인 결과
 *
 * 값은 "UP" 또는 "DOWN"이다.
 */
public record DepsStatus(String db, String redis) {

    private static final String UP = "UP";

    /**
     * 둘 다 UP인지
     */
    public boolean allUp() {
        return db.equals(UP) && redis.equals(UP);
    }
}

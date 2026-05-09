package com.ims.production.entity;

public enum SettlementResult {
    /** 모든 부품 재고 충분, 정상 차감 */
    SUCCESS,
    /** 재고 부족 / 재고 항목 없음 — 가능한 수량만 차감 후 이상 기록 */
    ANOMALY,
    /** BOM 탐색 실패 등 시스템 오류 — 재고 차감 없이 결산 종료 */
    FAILED
}

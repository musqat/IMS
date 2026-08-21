package com.ims.production.dto.response;

/**
 * 상태별 생산 기록 건수 집계
 */
public record ProductionCountsResponse(
        long pending,
        long settled,
        long cancelled,
        long anomaly,
        long total
) {
    public static ProductionCountsResponse of(long pending, long settled, long cancelled, long anomaly) {
        return new ProductionCountsResponse(
                pending, settled, cancelled, anomaly,
                pending + settled + cancelled);
    }
}

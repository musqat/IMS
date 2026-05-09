package com.ims.production.dto.response;

public record ProductionCountsResponse(
        long pending,
        long settled,
        long cancelled,
        long anomaly
) {
    public long total() {
        return pending + settled + cancelled;
    }
}

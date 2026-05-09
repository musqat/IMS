package com.ims.production.dto.response;

import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;

import java.time.LocalDateTime;

public record SettlementResponse(
        Long id,
        SettlementResult result,
        String anomalyDetail,
        String memo,
        LocalDateTime settledAt
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getResult(),
                settlement.getAnomalyDetail(),
                settlement.getMemo(),
                settlement.getSettledAt()
        );
    }
}

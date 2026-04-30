package com.ims.production.dto.response;

import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;

import java.time.LocalDateTime;

public record ProductionResponse(
        Long id,
        Long warehouseId,
        Long itemId,
        String itemName,
        int quantity,
        ProductionStatus status,
        SettlementResponse settlement,
        LocalDateTime createdAt
) {
    public static ProductionResponse from(ProductionRecord record, SettlementResponse settlement) {
        return new ProductionResponse(
                record.getId(),
                record.getWarehouse().getId(),
                record.getItem().getId(),
                record.getItem().getName(),
                record.getQuantity(),
                record.getStatus(),
                settlement,
                record.getCreatedAt()
        );
    }

    public static ProductionResponse from(ProductionRecord record) {
        return from(record, null);
    }
}

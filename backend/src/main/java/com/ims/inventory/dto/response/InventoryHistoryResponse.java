package com.ims.inventory.dto.response;

import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;

import java.time.LocalDateTime;

public record InventoryHistoryResponse(
        Long id,
        InventoryHistoryType type,
        int delta,
        String memo,
        LocalDateTime createdAt
) {
    public static InventoryHistoryResponse from(InventoryHistory history) {
        return new InventoryHistoryResponse(
                history.getId(),
                history.getType(),
                history.getDelta(),
                history.getMemo(),
                history.getCreatedAt()
        );
    }
}

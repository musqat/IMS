package com.ims.inventory.dto.response;

public record MaxProducibleResponse(
        Long itemId,
        String itemName,
        Integer maxQuantity  // null = BOM 없음(제한 없음), 0 이상 = 재고 기반 최대치
) {}

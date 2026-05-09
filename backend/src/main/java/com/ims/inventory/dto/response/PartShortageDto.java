package com.ims.inventory.dto.response;

public record PartShortageDto(
        Long partId,
        String partCode,
        String partName,
        long requiredPerUnit,   // 완성품 1개 생산 시 필요 수량
        int currentStock        // 현재 재고
) {}

package com.ims.item.dto.response;

import com.ims.item.entity.Bom;

public record BomResponse(
        Long id,
        Long parentItemId,
        String parentItemCode,
        String parentItemName,
        Long childItemId,
        String childItemCode,
        String childItemName,
        int quantity
) {
    public static BomResponse from(Bom bom) {
        return new BomResponse(
                bom.getId(),
                bom.getParent().getId(),
                bom.getParent().getItemCode(),
                bom.getParent().getName(),
                bom.getChild().getId(),
                bom.getChild().getItemCode(),
                bom.getChild().getName(),
                bom.getQuantity()
        );
    }
}

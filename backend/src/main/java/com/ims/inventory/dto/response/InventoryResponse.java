package com.ims.inventory.dto.response;

import com.ims.inventory.entity.Inventory;
import com.ims.item.entity.ItemType;

public record InventoryResponse(
        Long id,
        Long warehouseId,
        Long itemId,
        String itemCode,
        String itemName,
        ItemType itemType,
        int quantity,
        int safetyStock,
        String warning
) {
    private static final String WARNING_MESSAGE = "안전재고 이하입니다.";

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getWarehouse().getId(),
                inventory.getItem().getId(),
                inventory.getItem().getItemCode(),
                inventory.getItem().getName(),
                inventory.getItem().getType(),
                inventory.getQuantity(),
                inventory.getSafetyStock(),
                inventory.isBelowSafetyStock() ? WARNING_MESSAGE : null
        );
    }

}

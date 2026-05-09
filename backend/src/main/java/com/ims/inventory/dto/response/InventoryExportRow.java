package com.ims.inventory.dto.response;

import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;

import java.time.LocalDate;

public record InventoryExportRow(
        String itemCode,
        String itemName,
        InventoryHistoryType type,
        int delta,
        LocalDate date
) {

    /** InventoryHistory → InventoryExportRow 변환 */
    public static InventoryExportRow from(InventoryHistory history) {
        return new InventoryExportRow(
                history.getInventory().getItem().getItemCode(),
                history.getInventory().getItem().getName(),
                history.getType(),
                history.getDelta(),
                history.getCreatedAt().toLocalDate()
        );
    }
}

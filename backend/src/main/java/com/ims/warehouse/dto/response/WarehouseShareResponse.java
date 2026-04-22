package com.ims.warehouse.dto.response;

import com.ims.warehouse.entity.WarehouseShare;

public record WarehouseShareResponse(
        Long id,
        Long warehouseId,
        String warehouseName,
        Long sharedWithId,
        String sharedWithCompanyName,
        String permission
) {
    public static WarehouseShareResponse from(WarehouseShare share) {
        return new WarehouseShareResponse(
                share.getId(),
                share.getWarehouse().getId(),
                share.getWarehouse().getName(),
                share.getSharedWith().getId(),
                share.getSharedWith().getCompanyName(),
                share.getPermission().name()
        );
    }
}

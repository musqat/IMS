package com.ims.warehouse.dto.response;

import com.ims.warehouse.entity.Warehouse;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String name,
        String location,
        Long ownerId,
        String ownerCompanyName,
        LocalDateTime createdAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getLocation(),
                warehouse.getOwner().getId(),
                warehouse.getOwner().getCompanyName(),
                warehouse.getCreatedAt()
        );
    }
}

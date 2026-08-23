package com.ims.warehouse.dto.response;

import com.ims.warehouse.entity.Warehouse;

import java.time.LocalDateTime;

public record WarehouseResponse(
        Long id,
        String name,
        String location,
        Long ownerId,
        String ownerCompanyName,
        boolean active, // 활성 / 비활성 여부
        LocalDateTime createdAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getLocation(),
                warehouse.getOwner().getId(),
                warehouse.getOwner().getCompanyName(),
                warehouse.isActive(),
                warehouse.getCreatedAt()
        );
    }
}
